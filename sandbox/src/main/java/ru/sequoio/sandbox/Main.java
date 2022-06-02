package ru.sequoio.sandbox;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.sequoio.library.Sequoio;
import ru.sequoio.library.services.db.query.SupportedDatabases;

public class Main {

    public static void main(String[] args) {
        Sequoio sequoio = new Sequoio(
                "migrations/defence_copy",
                "production",
                "public",
                getDataSource(),
                SupportedDatabases.POSTGRES,
                false);
        sequoio.migrate();
    }

    private static DataSource getDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:54321/postgres");
        config.setDriverClassName("org.postgresql.Driver");
        config.setUsername("postgres");
        config.setPassword("postgres");
        return new HikariDataSource(config);
    }
}
