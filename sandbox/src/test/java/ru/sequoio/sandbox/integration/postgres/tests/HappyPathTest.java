package ru.sequoio.sandbox.integration.postgres.tests;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.sequoio.library.Sequoio;
import ru.sequoio.library.services.db.query.SupportedDatabases;
import ru.sequoio.sandbox.Migrator;
import ru.sequoio.sandbox.integration.postgres.SequoioTest;

public class HappyPathTest extends SequoioTest {

    @Test
    public void test() {
        Assertions.assertDoesNotThrow(() ->
            Migrator.doMigrate(new Sequoio(
                "migrations/happy_path",
                PRODUCTION_ENV,
                PUBLIC_SCHEMA,
                dataSource,
                SupportedDatabases.POSTGRES))
        );

        assertTableDDL("public", "users",
                List.of(
                        Column.of(1, "id", "BIGINT", false),
                        Column.of(2, "name", "TEXT", false)
                )
        );
    }
}