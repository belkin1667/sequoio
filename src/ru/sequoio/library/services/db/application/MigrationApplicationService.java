package ru.sequoio.library.services.db.application;

import javax.sql.DataSource;

import ru.sequoio.library.domain.Migration;

public interface MigrationApplicationService {

    void applyMigration(Migration migration);

    void setDefaultSchema(String defaultSchema);

    void setDataSource(DataSource dataSource);

    void init();

    void terminate();
}
