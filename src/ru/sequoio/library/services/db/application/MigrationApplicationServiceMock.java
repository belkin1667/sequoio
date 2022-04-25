package ru.sequoio.library.services.db.application;

import javax.sql.DataSource;

import ru.sequoio.library.domain.Migration;

public class MigrationApplicationServiceMock implements MigrationApplicationService {

    public void applyMigration(Migration migration) {
        System.out.println("Parsed migration: " + migration.toString() + "\n");
    }

    public void setDefaultSchema(String defaultSchema) {}

    public void setDataSource(DataSource dataSource) {}

    @Override
    public void init() {
        System.out.println("Init!");
    }

    @Override
    public void terminate() {
        System.out.println("Terminate!");
    }
}
