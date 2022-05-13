package ru.sequoio.library.services.db.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.graph.Graph;

public class MigrationApplicationServiceDryRun implements MigrationApplicationService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MigrationApplicationService.class);

    private void applyMigration(Migration migration) {
        LOGGER.info("[DRY RUN] Processing migration: {}", migration.getName());
    }

    @Override
    public void applyMigrationsFromGraph(Graph<Migration> migrationGraph) {
        migrationGraph.getOrderedNodes()
                .forEach(this::applyMigration);
    }
}
