package ru.sequoio.sandbox;

import ru.sequoio.library.Sequoio;

public class Migrator {

    public static void doMigrate(Sequoio sequoio) {
        if (sequoio == null) {
            return;
        }
        sequoio.migrate();
    }
}
