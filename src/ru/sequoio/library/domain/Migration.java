package ru.sequoio.library.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.sequoio.library.domain.graph.Node;
import ru.sequoio.library.domain.migration_paramters.BooleanParameterValue;
import ru.sequoio.library.domain.migration_paramters.MigrationParameter;
import ru.sequoio.library.domain.migration_paramters.ParameterValue;
import ru.sequoio.library.domain.migration_paramters.RunParameterValue;

/**
 * Single SQL expression
 */
public class Migration extends Node {

    private Path path;
    private String title;
    private String author;
    private String body;
    private Map<MigrationParameter, ParameterValue> params;
    private Map<String, String> userDefinedParams;
    private RunStatus runStatus;
    private MigrationLog loggedMigration;

    private Migration(Path path,
                     Integer naturalOrder,
                     String body,
                     String title,
                     String author,
                     Map<MigrationParameter, ParameterValue> params,
                     Map<String, String> userDefinedParams) {
        super(naturalOrder, title);
        this.path = path;
        this.title = title;
        this.author = author;
        this.params = params;
        this.userDefinedParams = userDefinedParams;
        setBody(body);
    }

    private void setBody(String body) {
        body = body.stripTrailing();
        this.body = body.charAt(body.length() - 1) == ';'
                        ? body
                        : body + ";";
    }

    public RunStatus getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(RunStatus runStatus) {
        this.runStatus = runStatus;
    }

    public MigrationLog getLoggedMigration() {
        return loggedMigration;
    }

    public void setLoggedMigration(MigrationLog loggedMigration) {
        this.loggedMigration = loggedMigration;
    }

    @Override
    public List<String> getExplicitPreviousNodeNames() {
        return Optional.ofNullable(params.get(MigrationParameter.RUN_AFTER).getValueAsString())
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<String> getExplicitNextNodeNames() {
        return Optional.ofNullable(params.get(MigrationParameter.RUN_BEFORE).getValueAsString())
                .map(List::of)
                .orElse(List.of());
    }

    public String getHash() {
        return body; //todo: add hashing here
    }

    public RunParameterValue getRunModifier() {
        return (RunParameterValue) params.get(MigrationParameter.RUN);
    }

    public String getEnvironment() {
        return params.get(MigrationParameter.ENVIRONMENT).getValueAsString();
    }

    public Boolean getIgnored() {
        return ((BooleanParameterValue) params.get(MigrationParameter.IGNORE)).getValue();
    }

    @Override
    public String toString() {
        return "Migration{" + "\n" +
                "path=" + path + "\n" +
                ", title=" + title + "\n" +
                ", author=" + author + "\n" +
                ", body=" + body + "\n" +
                ", params=" + params + "\n" +
                ", userDefinedParams=" + userDefinedParams + "\n" +
                '}';
    }

    public static MigrationBuilder builder() {
        return new MigrationBuilder();
    }


    public static class MigrationBuilder {

        private String title;
        private String author;

        private Map<MigrationParameter, ParameterValue> params;

        private Map<String, String> userDefinedParams;
        public MigrationBuilder header(String title,
                                       String author,
                                       Map<MigrationParameter, ParameterValue> params,
                                       Map<String, String> userDefinedParams) {
            this.title = title;
            this.author = author;
            this.params = params;
            this.userDefinedParams = userDefinedParams;
            return this;
        }

        public Migration build(Path path,
                               Integer naturalOrder,
                               String body) {
            if (title == null || author == null || params == null || userDefinedParams == null) {
                throw new IllegalStateException("Header is not set");
            }
            return new Migration(path, naturalOrder, body, title, author, params, userDefinedParams);
        }

    }
}
