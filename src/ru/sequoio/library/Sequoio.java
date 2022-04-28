package ru.sequoio.library;

import javax.sql.DataSource;

import ru.sequoio.library.domain.Migration;
import ru.sequoio.library.domain.graph.Graph;
import ru.sequoio.library.services.db.application.MigrationApplicationService;
import ru.sequoio.library.services.db.application.MigrationApplicationServiceImpl;
import ru.sequoio.library.services.db.query.SupportedDatabases;
import ru.sequoio.library.services.parsing.ChangelogParsingService;
import ru.sequoio.library.services.db.application.MigrationApplicationServiceMock;
import ru.sequoio.library.services.parsing.MigrationParsingService;

public class Sequoio {

    private ChangelogParsingService changelogParser;
    private MigrationParsingService migrationParser;
    private MigrationApplicationService migrationApplier;

    public Sequoio(String resourcesDirectory,
                   String environment,
                   String defaultSchema,
                   DataSource dataSource,
                   SupportedDatabases database,
                   boolean mock
    ) {
        this(resourcesDirectory, environment, defaultSchema, dataSource, database);
        if (mock) {
            this.migrationApplier = new MigrationApplicationServiceMock();
        }
    }
    
    public Sequoio(String resourcesDirectory,
                   String environment,
                   String defaultSchema,
                   DataSource dataSource,
                   SupportedDatabases database
    ) {
        this.migrationParser = new MigrationParsingService();
        this.changelogParser = new ChangelogParsingService(migrationParser, resourcesDirectory);
        this.migrationApplier = new MigrationApplicationServiceImpl(dataSource, defaultSchema, database.getQueryProvider(), environment);
    }

    /**
     * Entry point of library
     * Runs on Sequoio class creation
     */
    /*
        1. Parse all changelog files
        2. Parse all migration files
        3. Connect to database
        4. Apply migrations to database
    */
    public void init() {
        Graph<Migration> migrationGraph = changelogParser.parseChangelog();

        migrationApplier.apply(migrationGraph);
    }

    public void setResourcesDirectory(String resourcesDirectory) {
        changelogParser.setResourcesDirectory(resourcesDirectory);
    }

    public void setEnvironment(String environment) {
        migrationParser.setEnvironment(environment);
    }

    public void setDefaultSchema(String defaultSchema) {
        migrationApplier.setDefaultSchema(defaultSchema);
    }

    public void setDataSource(DataSource dataSource) {
        migrationApplier.setDataSource(dataSource);
    }

}


/*

1. Считываем чендж-лог файл = получаем пути к миграциям
2. Считываем файлы миграций = получаем SQL-выражения

 */
