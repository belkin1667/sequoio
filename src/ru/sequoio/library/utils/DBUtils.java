package ru.sequoio.library.utils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public class DBUtils {

    public static void prepare(PreparedStatement statement, List<Object> args) {
        prepare(statement, args.stream());
    }

    public static void prepare(PreparedStatement statement, Object[] args) {
        prepare(statement, Stream.of(args));
    }

    public static void prepare(PreparedStatement statement, Stream<Object> args) {
        StreamUtils.indexate(args).forEachOrdered(arg -> {
            try {
                statement.setObject(arg.getIndex() + 1, arg.getValue());
                var value = arg.getValue();
                var idx = arg.getIndex() + 1;

                if (value instanceof String) {
                    statement.setString(idx, (String) value);
                }
                else if (value instanceof Boolean) {
                    statement.setBoolean(idx, (boolean) value);
                }
                else if (value instanceof Long) {
                    statement.setLong(idx, (long) value);
                }
                else if (value instanceof Integer) {
                    statement.setInt(idx, (int) value);
                }
                else if (value instanceof Short) {
                    statement.setShort(idx, (short) value);
                }
                else if (value instanceof BigDecimal) {
                    statement.setBigDecimal(idx, (BigDecimal) value);
                }
                else if (value instanceof Timestamp) {
                    statement.setTimestamp(idx, (Timestamp) value);
                }
                else if (value instanceof Instant) {
                    statement.setTimestamp(idx, Timestamp.from((Instant) value));
                }
                else {
                    throw new UnsupportedOperationException(
                            String.format("Trying to prepare statement with unsupported value type %s",
                            value.getClass().getName())
                    );
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
