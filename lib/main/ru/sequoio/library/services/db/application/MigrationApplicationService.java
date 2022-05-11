package ru.sequoio.library.services.db.application;

import javax.sql.DataSource;

import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.graph.Graph;

public interface MigrationApplicationService {

    void setDefaultSchema(String defaultSchema);

    void setDataSource(DataSource dataSource);

    void apply(Graph<Migration> migrationGraph);
}
