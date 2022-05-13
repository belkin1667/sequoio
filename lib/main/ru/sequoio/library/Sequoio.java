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

    private final ChangelogParsingService changelogParser;
    private final MigrationApplicationService migrationApplier;

    public Sequoio(String resourcesDirectory,
                   String environment,
                   String defaultSchema,
                   DataSource dataSource,
                   SupportedDatabases database,
                   boolean dryRun
    ) {
        this.changelogParser = new ChangelogParsingService(new MigrationParsingService(), resourcesDirectory);
        if (dryRun) {
            this.migrationApplier = new MigrationApplicationServiceDryRun();
        } else {
            this.migrationApplier = new MigrationApplicationServiceImpl(
                    dataSource,
                    defaultSchema,
                    database.getQueryProvider(),
                    environment
            );
        }
    }
    
    public Sequoio(String resourcesDirectory,
                   String environment,
                   String defaultSchema,
                   DataSource dataSource,
                   SupportedDatabases database
    ) {
        this(resourcesDirectory, environment, defaultSchema, dataSource, database, false);
    }

    /**
     * Runs migration process
     */
    public void migrate() {
        migrationApplier.applyMigrationsFromGraph(changelogParser.parseChangelog());
    }

}
