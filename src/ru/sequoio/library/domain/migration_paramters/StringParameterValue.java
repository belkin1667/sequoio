package ru.sequoio.library.domain.migration_paramters;

public class StringParameterValue implements ParameterValue<String> {

    public static final StringParameterValue EMPTY = new StringParameterValue(null);

    private final String value;

    StringParameterValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public ParameterValue<String>[] parameterValues() {
        return new StringParameterValue[]{EMPTY};
    }

    @Override
    public ParameterValue<String> ofString(String paramValue) {
        if (paramValue == null) {
            return EMPTY;
        }
        return new StringParameterValue(paramValue);
    }

    @Override
    public String toString() {
        return getValueAsString();
    }
}
