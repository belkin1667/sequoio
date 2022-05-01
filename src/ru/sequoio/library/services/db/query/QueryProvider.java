package ru.sequoio.library.services.db.query;

public interface QueryProvider {

    String getCreateMigrationLogAndMigrationLogLockQuery(String migrationLogTableName, String migrationLogLockTableName);

    // Log queries
    String getTableExistsPreparedQuery();
    String getSelectMigrationLogPreparedQuery(String migrationLogTableName);
    String getInsertMigrationLogQuery(String migrationLogTableName);
    String getUpdateMigrationLogPreparedQuery(String migrationLogTableName);

    // Lock queries
    String getMigrationLogLockExistsQuery();
    String getCreateMigrationLogLockQuery();
    String getReleaseLockPreparedQuery(String migrationLogLockTableName);
    String getAcquireLockPreparedQuery(String migrationLogLockTableName);
    String getIsLockedPreparedQuery(String migrationLogLockTableName);

    String tryAcquireLockPreparedQuery();
}
