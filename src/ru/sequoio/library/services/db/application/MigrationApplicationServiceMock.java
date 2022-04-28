package ru.sequoio.library.services.db.application;

import javax.sql.DataSource;

import ru.sequoio.library.domain.Migration;
import ru.sequoio.library.domain.graph.Graph;

public class MigrationApplicationServiceMock implements MigrationApplicationService {

    private void applyMigration(Migration migration) {
        System.out.println("Parsed migration: " + migration.toString() + "\n");
    }

    public void setDefaultSchema(String defaultSchema) {}

    public void setDataSource(DataSource dataSource) {}

    @Override
    public void apply(Graph<Migration> migrationGraph) {
        migrationGraph.getOrderedNodes().stream()
                .map(node -> (Migration)node)
                .forEach(this::applyMigration);
    }
}
