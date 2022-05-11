package ru.sequoio.library.domain.migration.migration_paramters;

import java.io.Serializable;
import java.util.Objects;

public interface ParameterValue<T extends Serializable> {

    T getValue();

    ParameterValue<T>[] parameterValues();

    default String getValueAsString() {
        return Objects.isNull(getValue()) ? null : getValue().toString();
    }

    default ParameterValue<T> ofValue(T value) {
        return ofString(Objects.isNull(value) ? null : value.toString());
    }

    default ParameterValue<T> ofString(String paramValue) {
        for (var param : parameterValues()) {
            if (param.getValueAsString() == null && paramValue == null || param.getValueAsString().equals(paramValue)) {
                return param;
            }
        }
        throw new IllegalArgumentException(paramValue);
    }
}
