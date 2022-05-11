package ru.sequoio.library.services.parsing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.graph.Graph;
import ru.sequoio.library.utils.IOUtils;

public class ChangelogParsingService {

    private final static String CONFIG_HEADER = "--sequoio-configuration-file";

    private String[] classpath;
    private String sequoioResourcesDirectory;
    private MigrationParsingService migrationParser;

    public ChangelogParsingService(MigrationParsingService migrationParser,
                                   String sequoioResourcesDirectory) {
        this.migrationParser = migrationParser;
        this.classpath = System.getProperty("java.class.path").split(":");
        this.sequoioResourcesDirectory = sequoioResourcesDirectory;
    }

    /*
        1. Find all .seq files with correct headers
        2. Read all .seq files
            a) Split on migrations
            b) Parse migrations
     */
    public Graph<Migration> parseChangelog() {
        AtomicInteger index = new AtomicInteger();

        List<Migration> migrations =
                Arrays.stream(classpath).map(Path::of)
                .flatMap(p -> resolveResourcesPaths(p, sequoioResourcesDirectory)) // search only in specified 'sequoio config' directory
                .flatMap(this::listSequoioConfigFilesSafe) // find config files
                .distinct() // remove duplicates
                .filter(this::hasSequoioConfigHeader) // check file header
                .flatMap(this::parseMigrationFilePaths) // get all migrations
                .flatMap(p -> migrationParser.parseMigrations(p, index)) // parse all migrations in natural order
                .collect(Collectors.toList());

        return new Graph<>(migrations);
    }

    private Stream<Path> parseMigrationFilePaths(Path path) {
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

    private Stream<Path> resolveResourcesPaths(Path path, String maybeInnerFolder) {
        if (maybeInnerFolder == null) {
            return Stream.of(path);
        }
        return listSequoioResourcesDirectoriesSafe(path, maybeInnerFolder);
    }

    private Stream<Path> listSequoioResourcesDirectoriesSafe(Path path, String sequoioDirectory) {
        try {
            return listSequoioResourcesDirectories(path, sequoioDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<Path> listSequoioResourcesDirectories(Path path, String sequoioDirectory) throws IOException {
        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().endsWith(sequoioDirectory))
                    .collect(Collectors.toList());
        }
        return result.stream();
    }

    private Stream<Path> listSequoioConfigFilesSafe(Path path) {
        try {
            return listSequoioConfigFiles(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<Path> listSequoioConfigFiles(Path path) throws IOException {
        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".seq"))
                    .collect(Collectors.toList());
        }
        return result.stream();
    }

    public void setResourcesDirectory(String resourcesDirectory) {
        this.sequoioResourcesDirectory = resourcesDirectory;
    }
}
