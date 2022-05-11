package ru.sequoio.library.services.db.application.sieve;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sequoio.library.domain.migration.Migration;

public class SieveChain implements Sieve<Migration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SieveChain.class);

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
        boolean sifted = sieves.stream().map(filter -> filter.sift(migration)).reduce(true, (a, b) -> a && b);

        if (sifted) {
            LOGGER.debug("Migration {} was sifted and will be applied", migration.getName());
        } else {
            LOGGER.debug("Migration {} was not sifted and will not be applied", migration.getName());
        }

        return sifted;
    }
}
