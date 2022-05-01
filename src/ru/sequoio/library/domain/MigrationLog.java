package ru.sequoio.library.domain;

import java.time.Instant;

import ru.sequoio.library.domain.migration_paramters.RunParameterValue;

public class MigrationLog {

    private Instant createdAt;
    private Instant lastExecutedAt;
    private String runModifier;
    private String author;
    private String name;
    private String filename;
    private String hash;
    private Long runOrder;

    private boolean applied;

    public static final String createdAt_ = "created_at";
    public static final String lastExecutedAt_ = "last_executed_at";
    public static final String runModifier_ = "run_modifier";
    public static final String author_ = "author";
    public static final String name_ = "name";
    public static final String filename_ = "filename";
    public static final String hash_ = "hash";
    public static final String runOrder_ = "run_order";

    public MigrationLog(Instant createdAt,
                        Instant lastRunAt,
                        String runModifier,
                        String author,
                        String name,
                        String filename,
                        String hash,
                        Long runOrder
    ) {
        this.createdAt = createdAt;
        this.lastExecutedAt = lastRunAt;
        this.runModifier = runModifier;
        this.author = author;
        this.name = name;
        this.filename = filename;
        this.hash = hash;
        this.runOrder = runOrder;
    }

    public MigrationLog(String runModifier, String author, String name, String filename, String hash, Long runOrder) {
        this(null, null, runModifier, author, name, filename, hash, runOrder);
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

    public void setApplied() {
        applied = true;
    }

    public boolean isApplied() {
        return applied;
    }

    public boolean isNotApplied() {
        return !applied;
    }

    public String getFilename() {
        return filename;
    }

    public String getAuthor() {
        return author;
    }

    public Long getRunOrder() {
        return runOrder;
    }

    public void setRunModifier(String runModifier) {
        this.runModifier = runModifier;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setRunOrder(Long runOrder) {
        this.runOrder = runOrder;
    }
}
