package ru.sequoio.library.domain;

import java.time.Instant;

public class MigrationLog {

    private Instant createdAt;
    private Instant lastExecutedAt;
    private String runModifier;
    private String author;
    private String name;
    private String filename;
    private String hash;
    private Long order;

    public static final String createdAt_ = "created_at";
    public static final String lastExecutedAt_ = "last_executed_at";
    public static final String runModifier_ = "run_modifier";
    public static final String author_ = "author";
    public static final String name_ = "name";
    public static final String filename_ = "filename";
    public static final String hash_ = "hash";
    public static final String order_ = "order";

    public MigrationLog(Instant createdAt,
                        Instant lastRunAt,
                        String runModifier,
                        String author,
                        String name,
                        String filename,
                        String hash,
                        Long order
    ) {
        this.createdAt = createdAt;
        this.lastExecutedAt = lastRunAt;
        this.runModifier = runModifier;
        this.author = author;
        this.name = name;
        this.filename = filename;
        this.hash = hash;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public String getRunModifier() {
        return runModifier;
    }
}
