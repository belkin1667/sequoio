package ru.sequoio.library.domain.migration_paramters;

public enum BooleanParameterValue implements ParameterValue<Boolean> {
    TRUE(true),
    FALSE(false),
    ;

    private final Boolean value;

    BooleanParameterValue(boolean value) {
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public ParameterValue<Boolean>[] parameterValues() {
        return values();
    }
}
