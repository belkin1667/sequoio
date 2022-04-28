package demo;

import org.postgresql.ds.PGPoolingDataSource;
import ru.sequoio.library.Sequoio;
import ru.sequoio.library.services.db.query.SupportedDatabases;

public class Main {

    public static void main(String[] args) {

        PGPoolingDataSource dataSource = new PGPoolingDataSource();
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");
        dataSource.setDatabaseName("postgres");
        dataSource.setUrl("jdbc:postgresql://localhost:5433/");

        String env = "production";
        String defaultSchema = "public";
        String resourcesFolder = "conf";
        Sequoio sequoio = new Sequoio(resourcesFolder, env, defaultSchema, dataSource, SupportedDatabases.POSTGRES, false);
        sequoio.init();
    }
}
