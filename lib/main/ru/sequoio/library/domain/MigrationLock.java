package ru.sequoio.library.domain;

import java.time.Instant;

public class MigrationLock {

    private boolean locked;
    private Instant updatedAt;

    public static final String locked_ = "locked";
    public static final String updatedAt_ = "updated_at";


}
