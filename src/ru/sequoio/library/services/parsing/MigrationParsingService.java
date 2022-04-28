package ru.sequoio.library.services.parsing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.sequoio.library.domain.Migration;
import ru.sequoio.library.domain.migration_paramters.MigrationParameter;
import ru.sequoio.library.exception.InvalidFileFormatException;
import ru.sequoio.library.utils.IOUtils;

public class MigrationParsingService {

    public static String MIGRATION_FILE_HEADER = "--sequoio-migration-file";
    public static String MIGRATION_HEADER_REGEXP = "(--)(()|( ))(migration )([a-zA-Z0-9_\\- :#]+)";
    public static String MIGRATION_HEADER_REGEXP_CR = MIGRATION_HEADER_REGEXP + "(\n)";

    public Stream<Migration> parseMigrations(Path path, AtomicInteger order) {
        List<String> migrationHeaders = getMigrationHeaders(path);
        if (migrationHeaders.isEmpty()) {
            return Stream.empty();
        }
        return getMigrations(path, migrationHeaders, order);
    }

    private Stream<Migration> getMigrations(Path path, List<String> headers, AtomicInteger order) {
        AtomicInteger idx = new AtomicInteger(0);
        Scanner s = new Scanner(IOUtils.getInputStream(path));
        s.useDelimiter(MIGRATION_HEADER_REGEXP_CR);
        String migrationFileHeader = s.next();
        validateMigrationFileHeader(migrationFileHeader);

        List<Migration> migrations = new ArrayList<>();
        while (s.hasNext()) {
            String header = headers.get(idx.getAndIncrement()).strip();
            String body = s.next().strip();
            var migrationBuilder = Migration.builder();
            parseMigrationHeader(header, migrationBuilder);
            migrations.add(migrationBuilder.build(path, order.getAndIncrement(), body));
        }
        return migrations.stream();
    }

    private List<String> getMigrationHeaders(Path path) {
        Scanner s = new Scanner(IOUtils.getInputStream(path));
        return s.findAll(MIGRATION_HEADER_REGEXP_CR).map(MatchResult::group).collect(Collectors.toList());
    }

    private void parseMigrationHeader(String header, Migration.MigrationBuilder migrationBuilder) {
        String author = getAuthor(header);
        String title = getTitle(header);
        String startOfHeader = "--migration " + author + ":" + title;
        String paramsOnlyHeader = header.substring(startOfHeader.length()).strip();

        var params = MigrationParameter.getDefaultValuesParametersMap();
        Map<String, String> userDefinedParams = new HashMap<>();

        Arrays.stream(paramsOnlyHeader.split(" "))
                .filter(p -> p.contains(":"))
                .filter(p -> p.length() >= 3)
                .forEach(p -> {
                    String[] ps = p.split(":");
                    String paramName = ps[0];
                    String paramValue = ps[1];
                    Optional<MigrationParameter> maybeMigrationParameter = Arrays.stream(MigrationParameter.values())
                            .filter(mp -> mp.getName().equals(paramName))
                            .findFirst();
                    if (maybeMigrationParameter.isPresent()) {
                        var migrationParameter = maybeMigrationParameter.get();
                        params.remove(migrationParameter);
                        params.put(migrationParameter, migrationParameter.parseValue(paramValue));
                    } else if (paramName.startsWith("#")) {
                        userDefinedParams.put(paramName.substring(1), paramValue);
                    } else {
                        throw new IllegalArgumentException("Unknown parameter: " + paramName);
                    }
                });

        migrationBuilder.header(title, author, params, userDefinedParams);
    }

    private String getTitle(String header) {
        return header.split(":")[1].split(" ")[0];
    }

    private String getAuthor(String header) {
        return header.split(":")[0].split(" ")[1];
    }

    private void validateMigrationFileHeader(String header) {
        header = header == null ? null : header.strip();
        if (!MIGRATION_FILE_HEADER.equals(header)) {
            throw new InvalidFileFormatException("Migration file must have header {}", MIGRATION_FILE_HEADER);
        }
    }

}
