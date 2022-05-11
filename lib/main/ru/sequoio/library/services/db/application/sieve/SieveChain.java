package ru.sequoio.library.services.db.application.sieve;

import java.util.List;

import ru.sequoio.library.domain.migration.Migration;

public class SieveChain implements Sieve<Migration> {

    private final List<Sieve<Migration>> sieves;

    public SieveChain(String environment) {
        sieves = List.of(
                new IgnoreSieve(),
                new RunSieve(),
                new EnvironmentSieve(environment)
        );
    }

    @Override
    public boolean sift(Migration migration) {
        return sieves.stream().map(filter -> filter.sift(migration)).reduce(true, (a, b) -> a && b);
    }
}
