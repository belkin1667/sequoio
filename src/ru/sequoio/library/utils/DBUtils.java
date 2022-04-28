package ru.sequoio.library.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

public class DBUtils {

    public static void prepare(PreparedStatement statement, List<String> args) {
        prepare(statement, args.stream());
    }

    public static void prepare(PreparedStatement statement, String[] args) {
        prepare(statement, Stream.of(args));
    }

    public static void prepare(PreparedStatement statement, Stream<String> args) {
        StreamUtils.indexate(args).forEachOrdered(arg -> {
            try {
                statement.setString(arg.getIndex() + 1, arg.getValue());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
