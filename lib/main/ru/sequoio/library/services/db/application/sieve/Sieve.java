package ru.sequoio.library.services.db.application.sieve;

public interface Sieve<T> {

    /**
     * @return 'true' if object does match predicate, 'false' otherwise
     */
    boolean sift(T obj);

}
