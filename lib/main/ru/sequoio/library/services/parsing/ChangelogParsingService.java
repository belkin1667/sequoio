package ru.sequoio.library.services.parsing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.graph.Graph;
import ru.sequoio.library.services.db.application.MigrationApplicationService;
import ru.sequoio.library.utils.IOUtils;

public class ChangelogParsingService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MigrationApplicationService.class);

    private final static String CONFIG_HEADER = "--sequoio-configuration-file";
    private static final String CLASS_PATH_PROPERTY = "java.class.path";
    private static final String CLASS_PATH_SEPARATOR = ":";
    private static final String SEQUOIO_FILE_EXTENSION = ".seq";

    private final String[] classpath;
    private final String sequoioResourcesDirectory;
    private final MigrationParsingService migrationParser;

    public ChangelogParsingService(MigrationParsingService migrationParser,
                                   String sequoioResourcesDirectory) {
        this.migrationParser = migrationParser;
        this.classpath = System.getProperty(CLASS_PATH_PROPERTY).split(CLASS_PATH_SEPARATOR);
        this.sequoioResourcesDirectory = sequoioResourcesDirectory;
    }

    /**
     * Finds and reads from classpath all *.seq files with correct headers
     * Then splits them into migrations and constructs migration graph
     */
    public Graph<Migration> parseChangelog() {
        LOGGER.debug("Parsing changelog files in directory {} from classpath {}",
                sequoioResourcesDirectory, Arrays.toString(classpath));

        AtomicInteger index = new AtomicInteger();
        List<Migration> migrations =
                Arrays.stream(classpath)
                .map(Path::of)
                .flatMap(this::getResourceDirectories) // search only in specified 'sequoio config' directory
                .flatMap(this::getSequoioConfigFiles) // find config files in resource directories
                .distinct() // remove duplicates
                .filter(this::hasSequoioConfigHeader) // check file header in config files
                .flatMap(this::getMigrationFilePaths) // get all migrations paths
                .flatMap(p -> migrationParser.parseMigrations(p, index)) // parse all migrations in natural order
                .collect(Collectors.toList());

        LOGGER.debug("Found {} migrations", migrations.size());

        return new Graph<>(migrations);
    }

    private Stream<Path> getMigrationFilePaths(Path path) {
        LOGGER.debug("Parsing migration file paths in file {}", path);

        String dir = path.toFile().getAbsoluteFile().getParent();
        Scanner s = new Scanner(IOUtils.getInputStream(path));
        List<Path> migrationPaths = new ArrayList<>();
        while(s.hasNextLine()) {
            String line = s.nextLine().strip();
            if (line.startsWith("--") || line.isBlank()) {
                continue;   // skip comments and empty lines
            }
            migrationPaths.add(Path.of(dir, line));
        }

        LOGGER.debug("Found {} migration file paths for path {}", migrationPaths.size(), path);
        return migrationPaths.stream();
    }

    private boolean hasSequoioConfigHeader(Path path) {
        try {
            return CONFIG_HEADER.equals(getStrippedHeaderLine(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getStrippedHeaderLine(Path path) throws IOException {
        FileReader file = new FileReader(path.toString());
        try (BufferedReader buffer = new BufferedReader(file)) {
            return buffer.readLine().strip().toLowerCase(Locale.ROOT);
        }
    }

    private Stream<Path> getResourceDirectories(Path path) {
        LOGGER.debug("Getting resource directories for path {}...", path);

        if (!Files.isDirectory(path)) {
            LOGGER.debug("Path {} is not a directory!", path);
            return Stream.of();
        }
        Predicate<Path> filterPredicate = sequoioResourcesDirectory == null ?
                Files::isDirectory :
                p -> Files.isDirectory(p) && p.endsWith(sequoioResourcesDirectory);
        try {
            List<Path> result;
            try (Stream<Path> walk = Files.walk(path)) {
                var w = walk.collect(Collectors.toList());
                result = w.stream()
                        .filter(filterPredicate)
                        .collect(Collectors.toList());

            }
            LOGGER.debug("Found {} resource directories for path {}", result.size(), path);
            return result.stream();
        } catch (NoSuchFileException ex) {
            LOGGER.debug("Path {} not found!", path);
            return Stream.of();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<Path> getSequoioConfigFiles(Path path) {
        LOGGER.debug("Getting config files for path {}...", path);
        if (!Files.exists(path)) {
            return Stream.of();
        }
        try {
            List<Path> result;
            try (Stream<Path> walk = Files.walk(path)) {
                result = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(SEQUOIO_FILE_EXTENSION))
                        .collect(Collectors.toList());
            }
            LOGGER.debug("Found {} config files for path {}", result.size(), path);
            return result.stream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
