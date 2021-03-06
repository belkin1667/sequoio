package ru.sequoio.library.domain.migration.migration_paramters;

public enum RunParameterValue implements ParameterValue<String> {
    ONCE("once"),
    ALWAYS("always"),
    ONCHANGE("onchange")
    ;

    private final String value;

    RunParameterValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public ParameterValue<String>[] parameterValues() {
        return values();
    }
}
