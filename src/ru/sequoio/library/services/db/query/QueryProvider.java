package ru.sequoio.library.services.db.query;

public interface QueryProvider {

    // Log queries
    String getTableExistsQuery(String schemaName, String tableName);
    String getCreateMigrationLogQuery();
    String getMigrationLogQuery();

    // Lock queries
    String getMigrationLogLockExistsQuery();
    String getCreateMigrationLogLockQuery();
    String getReleaseLockQuery();
    String getAcquireLockQuery();
    String getIsLockedQuery();
    String tryAcquireLockQuery();

    String getCreateMigrationLogAndMigrationLogLockQuery();
}
