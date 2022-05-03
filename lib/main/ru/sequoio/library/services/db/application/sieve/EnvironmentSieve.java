package ru.sequoio.library.services.db.application.sieve;

import ru.sequoio.library.domain.Migration;

public class EnvironmentSieve implements Sieve<Migration> {

    private String environment;

    public EnvironmentSieve(String environment) {
        this.environment = environment;
    }

    @Override
    public boolean sift(Migration migration) {
        if (migration.getEnvironment() == null) {
            return true;
        }
        return migration.getEnvironment().equals(environment);
    }

}
