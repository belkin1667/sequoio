package ru.sequoio.library.services.db.application;

import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.graph.Graph;

public interface MigrationApplicationService {

    void applyMigrationsFromGraph(Graph<Migration> migrationGraph);
}
