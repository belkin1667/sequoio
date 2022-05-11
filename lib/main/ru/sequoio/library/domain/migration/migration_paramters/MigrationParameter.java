package ru.sequoio.library.domain.migration.migration_paramters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Parameters of migration
 * Defines the behaviour of migration applicator
 */
public enum MigrationParameter {

    RUN("run", RunParameterValue.ONCE),
    IGNORE("ignore", BooleanParameterValue.FALSE),
    TRANSACTIONAL("transactional", BooleanParameterValue.TRUE),
    FAIL_FAST("failFast", BooleanParameterValue.TRUE),
    RUN_AFTER("runAfter", StringParameterValue.EMPTY),
    RUN_BEFORE("runBefore", StringParameterValue.EMPTY),
    ENVIRONMENT("env", StringParameterValue.EMPTY)
    ;

    /*
      Assert that parameter names are unique
     */
    static {
        var isUnique = Arrays.stream(values()).map(MigrationParameter::getName).distinct().count() == values().length;
        if (!isUnique) {
            throw new IllegalStateException("MigrationParameter names must be unique");
        }
    }

    MigrationParameter(String name, ParameterValue defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    private final static Map<MigrationParameter, ParameterValue> DEFAULT_VALUE_PARAMETERS_MAP =
                    Arrays.stream(MigrationParameter.values())
                            .collect(Collectors.toMap(Function.identity(), MigrationParameter::getDefaultValue));

    private final String name;
    private final ParameterValue defaultValue;

    public String getName() {
        return name;
    }

    public ParameterValue getDefaultValue() {
        return defaultValue;
    }

    public static Map<MigrationParameter, ParameterValue> getDefaultValuesParametersMap() {
        return new HashMap<>(DEFAULT_VALUE_PARAMETERS_MAP);
    }

    public ParameterValue parseValue(String paramValue) {
        return defaultValue.ofString(paramValue);
    }
}
