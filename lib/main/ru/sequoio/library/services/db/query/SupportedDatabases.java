package ru.sequoio.library.services.db.query;

public enum SupportedDatabases {

    POSTGRES(new PostgresQueryProvider()),
    ;

    private final QueryProvider queryProvider;

    SupportedDatabases(QueryProvider queryProvider) {
        this.queryProvider = queryProvider;
    }

    public QueryProvider getQueryProvider() {
        return queryProvider;
    }
}
