package ru.sequoio.library.services.db.query;

import ru.sequoio.library.domain.migration.MigrationLock;
import ru.sequoio.library.domain.migration.MigrationLog;

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
                "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s) ",
            migrationLogTableName,
            MigrationLog.createdAt_,
            MigrationLog.lastExecutedAt_,
            MigrationLog.name_,
            MigrationLog.filename_,
            MigrationLog.author_,
            MigrationLog.runModifier_,
            MigrationLog.runOrder_,
            MigrationLog.hash_,
            MigrationLog.userDefinedParamsJson_
        ) + "VALUES (now(), now(), ?, ?, ?, ?, ?, ?, ?::JSON);";
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
                    "SET %s = now(), " +
                    "    %s = ?, " +
                    "    %s = ?, " +
                    "    %s = ?, " +
                    "    %s = ?, " +
                    "    %s = ? " +
                    "    %s = ?::JSON " +
                    "where %s = ?;",
                migrationLogTableName,
                MigrationLog.lastExecutedAt_,
                MigrationLog.filename_,
                MigrationLog.author_,
                MigrationLog.runModifier_,
                MigrationLog.runOrder_,
                MigrationLog.hash_,
                MigrationLog.userDefinedParamsJson_,
                MigrationLog.name_
            );
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
                    "SET %s = %b;",
                migrationLogLockTableName,
                MigrationLock.locked_,
                locked);
    }

    @Override
    public String getIsLockedPreparedQuery(String migrationLogLockTableName) {
        return String.format(
                    "SELECT %s " +
                    "FROM %s " +
                    "LIMIT 1;",
                MigrationLock.locked_,
                migrationLogLockTableName);
    }

    @Override
    public String getCreateMigrationLogAndMigrationLogLockQuery(String migrationLogTableName,
                                                                String migrationLogLockTableName) {
        return String.format(
                    "CREATE TABLE IF NOT EXISTS %s ( " +
                    "    %s     TEXT            PRIMARY KEY, " +
                    "    %s     TIMESTAMPTZ     NOT NULL,    " +
                    "    %s     TIMESTAMPTZ     NOT NULL,    " +
                    "    %s     TEXT            NOT NULL,    " +
                    "    %s     TEXT            NOT NULL,    " +
                    "    %s     TEXT            NOT NULL,    " +
                    "    %s     BIGINT          NOT NULL,    " +
                    "    %s     TEXT            NOT NULL,    " +
                    "    %s     JSON                         " +
                    ");",
                migrationLogTableName,
                MigrationLog.name_,
                MigrationLog.createdAt_,
                MigrationLog.lastExecutedAt_,
                MigrationLog.filename_,
                MigrationLog.author_,
                MigrationLog.runModifier_,
                MigrationLog.runOrder_,
                MigrationLog.hash_,
                MigrationLog.userDefinedParamsJson_)
            + STATEMENT_SEPARATOR
            + String.format(
                "CREATE TABLE IF NOT EXISTS %s ( " +
                "    %s BOOLEAN NOT NULL DEFAULT false " +
                ");",
                migrationLogLockTableName,
                MigrationLock.locked_)
            + STATEMENT_SEPARATOR
            + String.format(
                "INSERT INTO %s " +
                "DEFAULT VALUES;",
                migrationLogLockTableName);
    }
}
