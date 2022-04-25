package ru.sequoio.library.services.db.query;

public class PostgresQueryProvider implements QueryProvider {

    @Override
    public String getTableExistsQuery(String schemaName, String tableName) {
        return "SELECT count(*)=1 as isPresent " +
                "FROM pg_tables " +
                "where schemaname = '%s' and tablename = '%s';";
    }

    @Override
    public String getCreateMigrationLogQuery() {
        return null;
    }

    @Override
    public String getMigrationLogQuery() {
        return null;
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
    public String getReleaseLockQuery() {
        return null;
    }

    @Override
    public String getAcquireLockQuery() {
        return null;
    }

    @Override
    public String getIsLockedQuery() {
        return null;
    }

    @Override
    public String tryAcquireLockQuery() {
        return null;
    }

    @Override
    public String getCreateMigrationLogAndMigrationLogLockQuery() {
        return null;
    }
}
