package ru.sequoio.library.services.db.application;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.migration.MigrationLog;
import ru.sequoio.library.domain.migration.RunStatus;
import ru.sequoio.library.domain.graph.Graph;
import ru.sequoio.library.services.db.application.sieve.SieveChain;
import ru.sequoio.library.services.db.query.QueryProvider;
import ru.sequoio.library.utils.DBUtils;

public class MigrationApplicationServiceImpl implements MigrationApplicationService {


    public final static String MIGRATION_LOG_TABLE_NAME = "migration_log";
    public final static String MIGRATION_LOG_LOCK_TABLE_NAME = "migration_log_lock";
    public final static Integer LOCK_WAIT_TIME_MS = 1000;
    public final static Integer LOCK_WAIT_COUNTER_THRESHOLD = 15;

    private DataSource dataSource;
    private String defaultSchema;
    private QueryProvider queryProvider;
    private Map<String, MigrationLog> migrationLog;
    private int lockWaitCounter = 0;
    private SieveChain sieve;

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

    private boolean executeBooleanPreparedStatement(PreparedStatement statement, String columnName) throws SQLException {
        var resultSet = statement.executeQuery();
        resultSet.next();
        return resultSet.getBoolean(columnName);
    }

    /**
     * Checks if 'migration log' and 'migration log lock' tables exist <br/>
     * If they exist, acquire lock and return migration log <br/>
     * Else create tables
     */
    private List<MigrationLog> getOrCreateMigrationLog() throws SQLException, InterruptedException {
        boolean migrationLogExists, migrationLogLockExists;

        try (var conn = dataSource.getConnection()) {
            var statement = conn.prepareStatement(queryProvider.getTableExistsPreparedQuery());

            DBUtils.prepare(statement, List.of(defaultSchema, MIGRATION_LOG_TABLE_NAME));
            migrationLogExists = executeBooleanPreparedStatement(statement, "is_present");

            DBUtils.prepare(statement, List.of(defaultSchema, MIGRATION_LOG_LOCK_TABLE_NAME));
            migrationLogLockExists = executeBooleanPreparedStatement(statement, "is_present");
        }

        if (migrationLogExists && migrationLogLockExists) { //both exist
            acquireLock(); // guarantees that migration log will not be changed by other possible migration processes
            return getMigrationLog();
        } else if (!migrationLogExists && !migrationLogLockExists) { //both do not exist
            createMigrationLogAndLock();
            return List.of();
        } else { // one is missing
            String missingTable = migrationLogExists ? MIGRATION_LOG_LOCK_TABLE_NAME : MIGRATION_LOG_TABLE_NAME;
            throw new IllegalStateException(String.format("Illegal database state, table %s is missing!", missingTable));
        }
    }

    /**
     * Checks if lock is released now and, if so, acquires lock <br/>
     * Runs in single transaction <br/>
     *
     * @return 'true' if lock acquired successfully, 'false' otherwise
     */
    private boolean tryAcquireLock() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            var isLockedStatement = conn.prepareStatement(queryProvider.getIsLockedPreparedQuery(MIGRATION_LOG_LOCK_TABLE_NAME));
            var locked = executeBooleanPreparedStatement(isLockedStatement, "locked");
            if (locked) {
                conn.commit();
                return false;
            }
            var acquireLockStatement = conn.prepareStatement(queryProvider.getAcquireLockPreparedQuery(MIGRATION_LOG_LOCK_TABLE_NAME));
            acquireLockStatement.execute();
            conn.commit();
            return true;
        }
    }

    /**
     * Acquires lock with some number of retries and some delay time between tries
     */
    private void acquireLock() throws SQLException, InterruptedException {
        while (!tryAcquireLock()) {
            lockWaitCounter++;
            if (lockWaitCounter >= LOCK_WAIT_COUNTER_THRESHOLD) {
                throw new IllegalStateException(String.format("Could not acquire lock in %d seconds", lockWaitCounter * LOCK_WAIT_TIME_MS / 1000));
            }
            Thread.sleep(LOCK_WAIT_TIME_MS);
        }
    }

    /**
     * Releases lock
     */
    private void releaseLock() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            var statement  = conn.prepareStatement(queryProvider.getReleaseLockPreparedQuery(MIGRATION_LOG_LOCK_TABLE_NAME));
            statement.execute();
        }
    }

    /**
     * Loads a migration log and maps it to POJOs <br/>
     *
     * @return loaded migration log
     */
    private List<MigrationLog> getMigrationLog() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            List<MigrationLog> migrationLog = new ArrayList<>();

            var statement = conn.prepareStatement(queryProvider.getSelectMigrationLogPreparedQuery(MIGRATION_LOG_TABLE_NAME));
            var resultSet = statement.executeQuery();
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
                                resultSet.getLong(MigrationLog.runOrder_)
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
        try(var conn = dataSource.getConnection()) {
            String query = queryProvider.getCreateMigrationLogAndMigrationLogLockQuery(MIGRATION_LOG_TABLE_NAME, MIGRATION_LOG_LOCK_TABLE_NAME);
            var statement = conn.prepareStatement(query);
            statement.execute();
        }
    }

    private void applyMigration(Migration migration) {
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
                //log.warn("Failed to apply migration: {}", migration.getName(), e); todo: добавить логирование
            }
        }
    }

    private void addMigrationLog(Migration migration) throws SQLException {
        MigrationLog migrationLog = new MigrationLog(
                migration.getRunModifier().getValueAsString(),
                migration.getAuthor(),
                migration.getTitle(),
                migration.getPath().toString(),
                migration.getHash(),
                migration.getActualOrder());

        try (var conn = dataSource.getConnection()) {
            String query = queryProvider.getInsertMigrationLogQuery(MIGRATION_LOG_TABLE_NAME);
            var statement = conn.prepareStatement(query);
            DBUtils.prepare(statement,
                List.of(
                    migrationLog.getName(),
                    migrationLog.getFilename(),
                    migrationLog.getAuthor(),
                    migrationLog.getRunModifier(),
                    migrationLog.getHash(),
                    migrationLog.getRunOrder()
                ));
            statement.executeUpdate();
        }

        migration.setLoggedMigration(migrationLog);
    }

    private void updateMigrationLog(Migration migration) throws SQLException {
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
                        migrationLog.getName()
                    )
            );
            statement.executeUpdate();
        }
    }

    private void tryApplyMigration(Migration migration) {
        try {
            setRunStatusAndMigrationLog(migration);
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

    private Migration setRunStatusAndMigrationLog(Migration migration) {
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
        migration.setLoggedMigration(loggedMigration);

        return migration;
    }

    @Override
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected void init() {
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

    protected void terminate() {
        try {
            releaseLock();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apply(Graph<Migration> migrationGraph) {
        init();

        try {
            AtomicLong idx = new AtomicLong(0);
            migrationGraph.getOrderedNodes().stream()
                    .map(node -> (Migration) node)
                    .peek(migration -> migration.setActualOrder(idx.getAndIncrement()))
                    .forEach(this::tryApplyMigration);
            var notAppliedMigrations = migrationLog.values().stream()
                    .filter(MigrationLog::isNotApplied)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            if (notAppliedMigrations.size() > 0) {
                throw new IllegalStateException("Failed to apply migrations! There are possibly deleted migrations:\n"  + notAppliedMigrations);
            }
        } catch (Exception e) { //todo: удалить к чертям
            e.printStackTrace();
        }

        terminate();
    }
}
