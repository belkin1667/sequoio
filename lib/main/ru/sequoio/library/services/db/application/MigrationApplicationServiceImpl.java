package ru.sequoio.library.services.db.application;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.migration.MigrationLock;
import ru.sequoio.library.domain.migration.MigrationLog;
import ru.sequoio.library.domain.migration.RunStatus;
import ru.sequoio.library.domain.graph.Graph;
import ru.sequoio.library.services.db.application.sieve.SieveChain;
import ru.sequoio.library.services.db.query.QueryProvider;
import ru.sequoio.library.utils.DBUtils;

public class MigrationApplicationServiceImpl implements MigrationApplicationService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MigrationApplicationService.class);

    public final static String MIGRATION_LOG_TABLE_NAME = "migration_log";
    public final static String MIGRATION_LOG_LOCK_TABLE_NAME = "migration_log_lock";
    public final static Integer LOCK_WAIT_TIME_MS = 1000;
    public final static Integer LOCK_WAIT_COUNTER_THRESHOLD = 15;

    private final QueryProvider queryProvider;
    private final SieveChain sieve;
    private final DataSource dataSource;
    private final String defaultSchema;

    private Map<String, MigrationLog> migrationLog;

    public MigrationApplicationServiceImpl(
            DataSource dataSource,
            String defaultSchema,
            QueryProvider queryProvider,
            String environment
    ) {
        this.dataSource = dataSource;
        this.defaultSchema = defaultSchema;
        this.queryProvider = queryProvider;
        this.sieve = new SieveChain(environment);
    }

    @Override
    public void applyMigrationsFromGraph(Graph<Migration> migrationGraph) {
        LOGGER.debug("Applying migrations from graph");
        init();
        setAndValidateActualOrder(migrationGraph);
        tryApplyMigrations(migrationGraph);
        validateNotAppliedMigrations();
        terminate();
    }

    private void setAndValidateActualOrder(Graph<Migration> migrationGraph) {
        AtomicLong idx = new AtomicLong(0);
        var sortedOldMigrations = migrationGraph.getOrderedNodes().stream()
                .peek(migration -> migration.setActualOrder(idx.getAndIncrement()))
                .peek(this::setRunStatusAndMigrationLog)
                .filter(Migration::isNotNew)
                .sorted(Comparator.comparing(Migration::getActualOrder))
                .map(Migration::getTitle)
                .collect(Collectors.toList());
        var sortedLog = migrationLog.values().stream()
                .sorted(Comparator.comparing(MigrationLog::getRunOrder))
                .map(MigrationLog::getName)
                .collect(Collectors.toList());
        if (sortedOldMigrations.size() != sortedLog.size()) {
            throw new IllegalStateException("Failed to apply migrations! " +
                    "There are possibly deleted migrations");
        }
        if (!Objects.deepEquals(sortedOldMigrations, sortedLog)) {
            throw new IllegalStateException("Migration actual order have changed. " +
                    "Consider reviewing migrations with 'runBefore' and 'runAfter' parameters");
        }
    }

    private void tryApplyMigrations(Graph<Migration> migrationGraph) {
        migrationGraph.getOrderedNodes().forEach(this::tryApplyMigration);
    }

    private void validateNotAppliedMigrations() {
        var notAppliedMigrations = migrationLog.values().stream()
                .filter(MigrationLog::isNotApplied)
                .map(Objects::toString)
                .collect(Collectors.toList());
        if (notAppliedMigrations.size() > 0) {
            throw new IllegalStateException("Failed to apply migrations! " +
                    "There are possibly deleted migrations:\n"  + notAppliedMigrations);
        }
    }

    private void init() {
        LOGGER.debug("Initializing Sequoio migration task");
        try {
            migrationLog = getOrCreateMigrationLog().stream()
                    .collect(Collectors.toMap(MigrationLog::getName, Function.identity()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void terminate() {
        LOGGER.debug("Terminating Sequoio migration task");
        try {
            releaseLock();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void tryApplyMigration(Migration migration) {
        LOGGER.info("[MIGRATION] Processing migration: {}", migration.getName());
        try {
            boolean shouldBeApplied = sieve.sift(migration);
            if (shouldBeApplied) {
                applyMigration(migration);
                if (migration.getLoggedMigration() != null) {
                    updateMigrationLog(migration);
                }
                else {
                    addMigrationLog(migration);
                }
            }
            migration.getLoggedMigration().setApplied();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if 'migration log' and 'migration log lock' tables exist <br/>
     * If they exist, acquire lock and return migration log <br/>
     * Else create tables
     */
    private List<MigrationLog> getOrCreateMigrationLog() throws SQLException, InterruptedException {
        LOGGER.debug("Checking that 'migration log' and 'migration lock' exist...");

        boolean migrationLogExists, migrationLogLockExists;

        try (var conn = dataSource.getConnection()) {
            var statement = conn.prepareStatement(queryProvider.getTableExistsPreparedQuery());

            DBUtils.prepare(statement, List.of(defaultSchema, MIGRATION_LOG_TABLE_NAME));
            migrationLogExists = DBUtils.executeIsPresentPreparedStatement(statement);

            DBUtils.prepare(statement, List.of(defaultSchema, MIGRATION_LOG_LOCK_TABLE_NAME));
            migrationLogLockExists = DBUtils.executeIsPresentPreparedStatement(statement);
        }

        if (migrationLogExists && migrationLogLockExists) {
            LOGGER.debug("Both 'migration log' and 'migration lock' are present!");
            acquireLock();
            return getMigrationLog();
        } else if (!migrationLogExists && !migrationLogLockExists) {
            LOGGER.debug("Both 'migration log' and 'migration lock' are not present!");
            createMigrationLogAndLock();
            return List.of();
        } else { // one missing
            String missingTable = migrationLogExists ? MIGRATION_LOG_LOCK_TABLE_NAME : MIGRATION_LOG_TABLE_NAME;
            throw new IllegalStateException(String.format("Illegal database state, " +
                                                                "table %s is missing!", missingTable));
        }
    }

    /**
     * Checks if lock is released now and, if so, acquires lock <br/>
     * Runs in single transaction <br/>
     *
     * @return 'true' if lock acquired successfully, 'false' otherwise
     */
    private boolean tryAcquireLock(int attempt) throws SQLException {
        LOGGER.info("Trying to acquire lock. Attempt {}...", attempt);
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            var isLockedPreparedQuery = queryProvider.getIsLockedPreparedQuery(MIGRATION_LOG_LOCK_TABLE_NAME);
            var isLockedStatement = conn.prepareStatement(isLockedPreparedQuery);
            var locked = DBUtils.executeBooleanPreparedStatement(isLockedStatement, MigrationLock.locked_);
            if (locked) {
                conn.commit();
                LOGGER.debug("Failed to acquire lock. Already locked!");
                return false;
            }
            var acquireLockStatement = conn.prepareStatement(queryProvider.getAcquireLockPreparedQuery(MIGRATION_LOG_LOCK_TABLE_NAME));
            acquireLockStatement.execute();
            conn.commit();
            LOGGER.info("Lock acquired!");
            return true;
        }
    }

    /**
     * Acquires lock with some number of retries and some delay time between tries
     */
    private void acquireLock() throws SQLException, InterruptedException {
        LOGGER.debug("Acquiring lock");
        int attempt = 0;
        while (!tryAcquireLock(attempt)) {
            attempt++;
            if (attempt >= LOCK_WAIT_COUNTER_THRESHOLD) {
                throw new IllegalStateException(String.format("Could not acquire lock in %d seconds", attempt * LOCK_WAIT_TIME_MS / 1000));
            }
            Thread.sleep(LOCK_WAIT_TIME_MS);
        }
    }

    /**
     * Releases lock
     */
    private void releaseLock() throws SQLException {
        LOGGER.debug("Releasing lock");
        try (var conn = dataSource.getConnection()) {
            var releaseLockQuery = queryProvider.getReleaseLockPreparedQuery(MIGRATION_LOG_LOCK_TABLE_NAME);
            var releaseLockStatement  = conn.prepareStatement(releaseLockQuery);
            releaseLockStatement.execute();
        }
    }

    /**
     * Loads a migration log and maps it to POJOs <br/>
     *
     * @return loaded migration log
     */
    private List<MigrationLog> getMigrationLog() throws SQLException {
        LOGGER.debug("Getting migration log");
        try (var conn = dataSource.getConnection()) {
            List<MigrationLog> migrationLog = new ArrayList<>();

            var selectMigrationLogQuery = queryProvider.getSelectMigrationLogPreparedQuery(MIGRATION_LOG_TABLE_NAME);
            var selectMigrationLogStatement = conn.prepareStatement(selectMigrationLogQuery);
            var resultSet = selectMigrationLogStatement.executeQuery();
            resultSet.next();
            if (!resultSet.isFirst()) {
                return migrationLog;
            }
            do {
                migrationLog.add(
                        new MigrationLog(
                                resultSet.getTimestamp(MigrationLog.createdAt_).toInstant(),
                                resultSet.getTimestamp(MigrationLog.lastExecutedAt_).toInstant(),
                                resultSet.getString(MigrationLog.runModifier_),
                                resultSet.getString(MigrationLog.author_),
                                resultSet.getString(MigrationLog.name_),
                                resultSet.getString(MigrationLog.filename_),
                                resultSet.getString(MigrationLog.hash_),
                                resultSet.getLong(MigrationLog.runOrder_),
                                resultSet.getString(MigrationLog.userDefinedParamsJson_)
                        )
                );
            } while (resultSet.next());

            return migrationLog;
        }
    }

    /**
     * Creates 'migration log' and 'migration log lock' tables
     */
    private void createMigrationLogAndLock() throws SQLException {
        LOGGER.debug("Creating 'migration log' and 'migration lock' tables");
        try(var conn = dataSource.getConnection()) {
            String createMigrationLogAndLockTablesQuery = queryProvider.getCreateMigrationLogAndMigrationLogLockQuery(
                    MIGRATION_LOG_TABLE_NAME,
                    MIGRATION_LOG_LOCK_TABLE_NAME
            );
            var statement = conn.prepareStatement(createMigrationLogAndLockTablesQuery);
            statement.execute();
        }
    }

    private void applyMigration(Migration migration) {
        LOGGER.debug("Applying migration: {}", migration.getName());
        try {
            List<String> statements = migration.getStatements();
            if (migration.isTransactional()) {
                try (var conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    for (String statement : statements) {
                        PreparedStatement preparedStatement = conn.prepareStatement(statement);
                        preparedStatement.execute();
                    }
                    conn.commit();
                }
            } else {
                try (var conn = dataSource.getConnection()) {
                    for (String statement : statements) {
                        PreparedStatement preparedStatement = conn.prepareStatement(statement);
                        preparedStatement.execute();
                    }
                }
            }
        } catch (SQLException e) {
            if (migration.isFailOnError()) {
                throw new RuntimeException(e);
            } else {
                LOGGER.warn("Failed to apply migration: {}", migration.getName(), e);
            }
        }
    }

    private void addMigrationLog(Migration migration) throws SQLException {
        LOGGER.debug("Adding new migration log record for migration {}", migration.getName());
        MigrationLog migrationLog = new MigrationLog(
                migration.getRunModifier().getValueAsString(),
                migration.getAuthor(),
                migration.getTitle(),
                migration.getPath().toString(),
                migration.getHash(),
                migration.getActualOrder(),
                migration.getUserDefinedParams());

        try (var conn = dataSource.getConnection()) {
            String query = queryProvider.getInsertMigrationLogQuery(MIGRATION_LOG_TABLE_NAME);
            var statement = conn.prepareStatement(query);
            DBUtils.prepare(statement,
                List.of(
                    migrationLog.getName(),
                    migrationLog.getFilename(),
                    migrationLog.getAuthor(),
                    migrationLog.getRunModifier(),
                    migrationLog.getRunOrder(),
                    migrationLog.getHash(),
                    migrationLog.getUserDefinedParamsJson()
                ));
            statement.executeUpdate();
        }

        migration.setLoggedMigration(migrationLog);
    }

    private void updateMigrationLog(Migration migration) throws SQLException {
        LOGGER.debug("Updating migration log record for migration {}", migration.getName());
        migration.updateMigrationLog();
        MigrationLog migrationLog = migration.getLoggedMigration();

        try (var conn = dataSource.getConnection()) {
            String query = queryProvider.getUpdateMigrationLogPreparedQuery(MIGRATION_LOG_TABLE_NAME);
            var statement = conn.prepareStatement(query);
            DBUtils.prepare(statement,
                    List.of(
                        migrationLog.getFilename(),
                        migrationLog.getAuthor(),
                        migrationLog.getRunModifier(),
                        migrationLog.getRunOrder(),
                        migrationLog.getHash(),
                        migrationLog.getName(),
                        migrationLog.getUserDefinedParamsJson()
                    )
            );
            statement.executeUpdate();
        }
    }

    private void setRunStatusAndMigrationLog(Migration migration) {
        LOGGER.debug("Setting run status and migration log for migration {}", migration.getName());

        var loggedMigration = migrationLog.get(migration.getName());
        if (loggedMigration == null) {
            migration.setRunStatus(RunStatus.NEW);
        }
        else if (loggedMigration.getHash().equals(migration.getHash())) {
            migration.setRunStatus(RunStatus.APPLIED);
        }
        else {
            migration.setRunStatus(RunStatus.BODY_CHANGED);
        }
        LOGGER.debug("Migration {} has '{}' run status", migration.getName(), migration.getRunStatus());

        migration.setLoggedMigration(loggedMigration);
    }
}
