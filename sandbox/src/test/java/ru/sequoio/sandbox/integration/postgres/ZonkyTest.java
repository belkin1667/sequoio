package ru.sequoio.sandbox.integration.postgres;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import ru.sequoio.library.utils.DBUtils;

public abstract class ZonkyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkyTest.class);

    public static EmbeddedPostgres pg;
    public static DataSource dataSource;
    public static boolean autoKillConflictingDatabase = false;
    public static boolean checkConflictingPostgres = true;

    @BeforeEach
    void setUp() throws Exception {
        var pgPort = 54321;

        if (checkConflictingPostgres) {
            var runtime = Runtime.getRuntime();
            var proc = runtime.exec(new String[] {
                    "sh", "-c", String.format("(lsof -PiTCP -sTCP:LISTEN | grep postgres | grep %s)", pgPort)
            });
            Scanner s = new Scanner(proc.getInputStream());
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.contains("postgres") && line.contains(String.valueOf(pgPort))) {
                    LOGGER.warn(() -> String.format("Found running postgres database on port %s!", pgPort));
                    if (autoKillConflictingDatabase) {
                        var pid = line.split(" ")[2];
                        LOGGER.warn(() -> String.format("Killing conflicting database with pid %s", pid));
                        runtime.exec(String.format("kill -9 %s", pid));
                    }
                }
            }
        }

        LOGGER.info(() -> String.format("Starting embedded postgres on port %s", pgPort));
        pg = EmbeddedPostgres.builder()
                .setPort(pgPort)
                .setCleanDataDirectory(true)
                .start();
        dataSource = pg.getPostgresDatabase();
    }

    @AfterEach
    void tearDownEmbeddedPostgres() throws IOException {
        if (pg != null) {
            LOGGER.info(() -> "Shutting down postgres");
            pg.close();
        }
    }

    public static void assertTableDDL(String schemaName, String tableName, List<Column> expectedColumns){
        Assertions.assertDoesNotThrow(() -> assertTableDDLThrows(schemaName, tableName, expectedColumns));
    }

    private static void assertTableDDLThrows(String schemaName, String tableName, List<Column> expectedColumns) throws SQLException {
        if (expectedColumns == null) {
            expectedColumns = List.of();
        }
        var actualColumns = extractActualColumns(schemaName, tableName);
        Assertions.assertIterableEquals(expectedColumns, actualColumns);
    }

    private static List<Column> extractActualColumns(String schemaName, String tableName) throws SQLException {
        try(var conn = dataSource.getConnection()) {
            String query =
                    "SELECT ordinal_position, column_name, data_type, is_nullable " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = ? and table_name = ?;";
            var statement = conn.prepareStatement(query);
            DBUtils.prepare(statement, List.of(schemaName, tableName));
            var resultSet = statement.executeQuery();
            List<Column> actualColumns = new ArrayList<>();
            if (!resultSet.isBeforeFirst()) {
                return actualColumns;
            }
            while (resultSet.next()) {
                actualColumns.add(
                        new Column(
                                resultSet.getInt(Column.ordinalPosition_),
                                resultSet.getString(Column.name_),
                                resultSet.getString(Column.dataType_),
                                resultSet.getString(Column.isNullable_)
                        )
                );
            }
            return actualColumns;
        }
    }

    public static class Column {

        public static final String ordinalPosition_ = "ordinal_position";
        public static final String name_ = "column_name";
        public static final String dataType_ = "data_type";
        public static final String isNullable_ = "is_nullable";

        private final Integer ordinalPosition;
        private final String name;
        private final String dataType;
        private final String isNullable;

        public Column(int ordinalPosition, String name, String dataType, String isNullable) {
            this.ordinalPosition = ordinalPosition;
            this.name = name;
            this.dataType = dataType;
            this.isNullable = isNullable;
        }

        public static Column of(int pos, String name, String dataType, boolean isNullable) {
            return new Column(pos, name, dataType, isNullable ? "YES" : "NO");
        }

        public Integer getOrdinalPosition() {
            return ordinalPosition;
        }

        public String getName() {
            return name;
        }

        public String getDataType() {
            return dataType;
        }

        public String getIsNullable() {
            return isNullable;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Column) {
                Column other = ((Column) obj);
                return this.dataType.equalsIgnoreCase(other.getDataType()) &&
                        this.isNullable.equalsIgnoreCase(other.getIsNullable()) &&
                        this.ordinalPosition.equals(other.getOrdinalPosition()) &&
                        this.name.equalsIgnoreCase(other.getName());
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("Column(dataType=%s, isNullable=%s, ordinalPosition=%s, name=%s)",
                                         dataType,    isNullable,    ordinalPosition,    name);
        }
    }

}
