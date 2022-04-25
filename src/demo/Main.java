package demo;

import javax.sql.DataSource;

import ru.sequoio.library.Sequoio;
import ru.sequoio.library.services.db.query.SupportedDatabases;
import org.postgresql.*;

public class Main {

    public static void main(String[] args) {
        DataSource dataSource = null;
        String env = "production";
        String defaultSchema = "public";
        String resourcesFolder = "conf";
        Sequoio sequoio = new Sequoio(resourcesFolder, env, defaultSchema, dataSource, SupportedDatabases.POSTGRES, false);
        sequoio.init();
    }
}
