package ru.sequoio.library.services.db.query;

import ru.sequoio.library.domain.MigrationLog;

public class PostgresQueryProvider implements QueryProvider {

    private static final String STATEMENT_SEPARATOR = "\n";

    @Override
    public String getTableExistsPreparedQuery() {
        return "SELECT count(*)=1 as is_present " +
                "FROM pg_tables " +
                "where schemaname = ? and tablename = ?;";
    }

    @Override
    public String getInsertMigrationLogQuery(String migrationLogTableName) {
        return String.format(
                "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s) ",
            migrationLogTableName,
            MigrationLog.name_,
            MigrationLog.filename_,
            MigrationLog.author_,
            MigrationLog.runModifier_,
            MigrationLog.hash_,
            MigrationLog.runOrder_,
            MigrationLog.createdAt_,
            MigrationLog.lastExecutedAt_
        ) + "VALUES (?, ?, ?, ?, ?, ?, now(), now());";
    }

    @Override
    public String getSelectMigrationLogPreparedQuery(String migrationLogTableName) {
        return String.format(
                    "SELECT * " +
                    "FROM %s;"
                , migrationLogTableName);
    }

    @Override
    public String getUpdateMigrationLogPreparedQuery(String migrationLogTableName) {
        return String.format(
                    "UPDATE %s " +
                    "SET last_executed_at = now(), " +
                    "    filename = ?, " +
                    "    author = ?, " +
                    "    run_modifier = ?, " +
                    "    run_order = ?, " +
                    "    hash = ? " +
                    "where name = ?;"
                , migrationLogTableName);
    }

    @Override
    public String getMigrationLogLockExistsQuery() {
        return null;
    }

    @Override
    public String getCreateMigrationLogLockQuery() {
        return null;
    }

    @Override
    public String getReleaseLockPreparedQuery(String migrationLogLockTableName) {
        return getLockUpdatePreparedQuery(migrationLogLockTableName, false);
    }

    @Override
    public String getAcquireLockPreparedQuery(String migrationLogLockTableName) {
        return getLockUpdatePreparedQuery(migrationLogLockTableName, true);
    }

    private String getLockUpdatePreparedQuery(String migrationLogLockTableName, boolean locked) {
        return String.format(
                    "UPDATE %s " +
                    "SET locked = %b;"
                , migrationLogLockTableName, locked);
    }

    @Override
    public String getIsLockedPreparedQuery(String migrationLogLockTableName) {
        return String.format(
                    "SELECT locked " +
                    "FROM %s " +
                    "LIMIT 1;"
                , migrationLogLockTableName);
    }

    @Override
    public String tryAcquireLockPreparedQuery() {
        return null;
    }

    @Override
    public String getCreateMigrationLogAndMigrationLogLockQuery(String migrationLogTableName,
                                                                String migrationLogLockTableName) {
        return String.format(
                    "CREATE TABLE IF NOT EXISTS %s ( " +
                    "    name                TEXT            PRIMARY KEY, " +
                    "    filename            TEXT            NOT NULL,    " +
                    "    author              TEXT            NOT NULL,    " +
                    "    run_modifier        TEXT            NOT NULL,    " +
                    "    run_order           BIGINT          NOT NULL,    " +
                    "    hash                TEXT            NOT NULL,    " +
                    "    created_at          TIMESTAMPTZ     NOT NULL,    " +
                    "    last_executed_at    TIMESTAMPTZ     NOT NULL     " +
                    ");"
                , migrationLogTableName)
                + STATEMENT_SEPARATOR
                + String.format(
                    "CREATE TABLE IF NOT EXISTS %s ( " +
                    "    locked BOOLEAN NOT NULL DEFAULT false " +
                    ");"
                , migrationLogLockTableName)
                + STATEMENT_SEPARATOR
                + String.format(
                    "INSERT INTO %s " +
                    "DEFAULT VALUES;"
                , migrationLogLockTableName);
    }
}
