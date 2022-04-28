package ru.sequoio.library.services.db.query;

public interface QueryProvider {

    // Log queries
    String getTableExistsPreparedQuery();
    String getCreateMigrationLogQuery();
    String getMigrationLogPreparedQuery(String migrationLogTableName);

    // Lock queries
    String getMigrationLogLockExistsQuery();
    String getCreateMigrationLogLockQuery();
    String getReleaseLockPreparedQuery(String migrationLogLockTableName);
    String getAcquireLockPreparedQuery(String migrationLogLockTableName);
    String getIsLockedPreparedQuery(String migrationLogLockTableName);
    String tryAcquireLockPreparedQuery();

    String getCreateMigrationLogAndMigrationLogLockQuery(String migrationLogTableName, String migrationLogLockTableName);
}
