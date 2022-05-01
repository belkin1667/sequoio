package ru.sequoio.library.services.db.application.sieve;

import ru.sequoio.library.domain.Migration;

public class IgnoreSieve implements Sieve<Migration> {

    @Override
    public boolean sift(Migration migration) {
        boolean isIgnored = migration.getIgnored();
        if (isIgnored && migration.getLoggedMigration() != null) {
            throw new IllegalArgumentException("Can not ignore already applied migration!");
        }
        else {
            return !isIgnored;
        }
    }

}
