package ru.sequoio.sandbox;

import ru.sequoio.library.Sequoio;
import ru.sequoio.library.services.db.query.SupportedDatabases;

public class Main {

    public static void main(String[] args) {
        Migrator.doMigrate(new Sequoio("", "", "", null, SupportedDatabases.POSTGRES));
    }
}
