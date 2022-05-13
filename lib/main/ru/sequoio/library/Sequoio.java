package ru.sequoio.library;

import javax.sql.DataSource;

import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.graph.Graph;
import ru.sequoio.library.services.db.application.MigrationApplicationService;
import ru.sequoio.library.services.db.application.MigrationApplicationServiceImpl;
import ru.sequoio.library.services.db.query.SupportedDatabases;
import ru.sequoio.library.services.parsing.ChangelogParsingService;
import ru.sequoio.library.services.db.application.MigrationApplicationServiceDryRun;
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
                   boolean dryRun
    ) {
        this(resourcesDirectory, environment, defaultSchema, dataSource, database);
        if (dryRun) {
            this.migrationApplier = new MigrationApplicationServiceDryRun();
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
    public void init() {
        Graph<Migration> migrationGraph = changelogParser.parseChangelog();

        migrationApplier.applyMigrationsFromGraph(migrationGraph);
    }

}
