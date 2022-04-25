package ru.sequoio.library.services.db.application;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import ru.sequoio.library.domain.Migration;
import ru.sequoio.library.domain.MigrationLog;
import ru.sequoio.library.services.db.query.QueryProvider;

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

    public MigrationApplicationServiceImpl(DataSource dataSource, String defaultSchema, QueryProvider queryProvider) {
        this.dataSource = dataSource;
        this.defaultSchema = defaultSchema;
        this.queryProvider = queryProvider;
    }

    /**
     * check if migration_log and migration_log_lock tables exist
     * create them or just return existing
     */
    private List<MigrationLog> getOrCreateMigrationLog() throws SQLException, InterruptedException {
        var conn = dataSource.getConnection();
        var statement  = conn.createStatement();
        var migrationLogExists = statement.executeQuery(queryProvider.getTableExistsQuery(defaultSchema, MIGRATION_LOG_TABLE_NAME)).getBoolean(0);
        var migrationLogLockExists = statement.executeQuery(queryProvider.getTableExistsQuery(defaultSchema, MIGRATION_LOG_LOCK_TABLE_NAME)).getBoolean(0);
        if (migrationLogExists && migrationLogLockExists) { //both exist
            //acquireLock(); // guarantees that migration log will not change
            //return getMigrationLog();
            return List.of();
        } else if (!migrationLogExists && !migrationLogLockExists) { //both do not exist
            //createMigrationLog();
            return List.of();
        } else { // one is missing
            String missingTable = migrationLogExists ? MIGRATION_LOG_LOCK_TABLE_NAME : MIGRATION_LOG_TABLE_NAME;
            throw new IllegalStateException(String.format("Illegal database state, table %s is missing!", missingTable));
        }
    }

    private boolean isLocked() throws SQLException {
        var conn = dataSource.getConnection();
        var statement  = conn.createStatement();
        return statement.executeQuery(queryProvider.getIsLockedQuery()).getBoolean("lock");
    }

    private boolean tryAcquireLock() throws SQLException {
        var conn = dataSource.getConnection();
        var statement  = conn.createStatement();
        return statement.executeQuery(queryProvider.tryAcquireLockQuery()).getBoolean("lock");
    }

    private void acquireLock() throws SQLException, InterruptedException {
        while (!tryAcquireLock()) {
            lockWaitCounter++;
            if (lockWaitCounter >= LOCK_WAIT_COUNTER_THRESHOLD) {
                throw new IllegalStateException(String.format("Could not acquire lock in %d number of tries", lockWaitCounter));
            }
            Thread.sleep(LOCK_WAIT_TIME_MS);
        }
    }

    private void releaseLock() throws SQLException {
        var conn = dataSource.getConnection();
        var statement  = conn.createStatement();
        statement.executeQuery(queryProvider.getReleaseLockQuery());
    }

    private List<MigrationLog> getMigrationLog() throws SQLException {
        List<MigrationLog> migrationLog = new ArrayList<>();

        var conn = dataSource.getConnection();
        var statement  = conn.createStatement();
        var resultset = statement.executeQuery(queryProvider.getMigrationLogQuery());

        if (!resultset.isFirst()) {
            return migrationLog;
        }
        do {
            migrationLog.add(
                new MigrationLog(
                    resultset.getTimestamp(MigrationLog.createdAt_).toInstant(),
                    resultset.getTimestamp(MigrationLog.lastExecutedAt_).toInstant(),
                    resultset.getString(MigrationLog.runModifier_),
                    resultset.getString(MigrationLog.author_),
                    resultset.getString(MigrationLog.name_),
                    resultset.getString(MigrationLog.filename_),
                    resultset.getString(MigrationLog.hash_),
                    resultset.getLong(MigrationLog.order_)
                )
            );
        } while (resultset.next());

        return migrationLog;
    }

    private void createMigrationLog() throws SQLException {
        var conn = dataSource.getConnection();
        var statement  = conn.createStatement();
        statement.execute(queryProvider.getCreateMigrationLogAndMigrationLogLockQuery());
    }

    @Override
    public void applyMigration(Migration migration) {
        var loggedMigration = migrationLog.get(migration.getName());
        if (loggedMigration.getHash().equals(migration.getHash())) {

        }
        else {

        }

        //todo: расписать стратегии применения миграций
    }

    @Override
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void init() {
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

    @Override
    public void terminate() {
        try {
            releaseLock();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
