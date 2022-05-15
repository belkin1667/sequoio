package ru.sequoio.sandbox.integration.postgres;

public abstract class SequoioTest extends ZonkyTest {

    public static final String PRODUCTION_ENV = "production";
    public static final String TESTING_ENV = "testing";
    public static final String TESTS_ENV = "tests";
    public static final String LOCAL_ENV = "local";

    public static final String PUBLIC_SCHEMA = "public";

    static {
        autoKillConflictingDatabase = true;
    }
}
