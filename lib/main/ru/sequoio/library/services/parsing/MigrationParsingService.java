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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.migration.migration_paramters.MigrationParameter;
import ru.sequoio.library.exception.InvalidFileFormatException;
import ru.sequoio.library.utils.IOUtils;

public class MigrationParsingService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MigrationParsingService.class);
    private static final String USER_DEFINED_PARAM_NAME_PREFIX = "#";
    private static final String HEADER_START_TEMPLATE = "--migration %s:%s";
    private static final String PARAMS_SPLIT_REGEXP = " ";
    private static final String MIGRATION_FILE_HEADER = "--sequoio-migration-file";
    private static final String MIGRATION_HEADER_REGEXP = "(--migration )([a-zA-Z0-9_\\- :#]+)";
    private static final String MIGRATION_HEADER_REGEXP_CR = MIGRATION_HEADER_REGEXP + "(\n)";
    private static final String PARAMS_KEY_VALUE_SEPARATOR = ":";

    public Stream<Migration> parseMigrations(Path path, AtomicInteger order) {
        LOGGER.debug("Parsing migrations in file {}", path);

        List<String> migrationHeaders = getMigrationHeaders(path);
        if (migrationHeaders.isEmpty()) {
            return Stream.empty();
        }
        return getMigrations(path, migrationHeaders, order);
    }

    private List<String> getMigrationHeaders(Path path) {
        LOGGER.debug("Getting migrations headers in file {}", path);
        Scanner s = new Scanner(IOUtils.getInputStream(path));
        var headers = s.findAll(MIGRATION_HEADER_REGEXP_CR)
                                    .map(MatchResult::group)
                                    .collect(Collectors.toList());
        LOGGER.debug("Found {} migration headers in file {}", headers.size(), path);
        return headers;
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
        LOGGER.debug("Found {} migrations in file {}", migrations.size(), path);

        assert migrations.size() == headers.size();
        return migrations.stream();
    }

    private void parseMigrationHeader(String header, Migration.MigrationBuilder migrationBuilder) {
        LOGGER.debug("Parsing migration header {}", header);

        String author = getAuthor(header);
        String title = getTitle(header);
        String startOfHeader = String.format(HEADER_START_TEMPLATE, author, title);
        String paramsOnlyHeader = header.substring(startOfHeader.length()).strip();

        var params = MigrationParameter.getDefaultValuesParametersMap();
        Map<String, String> userDefinedParams = new HashMap<>();

        Arrays.stream(paramsOnlyHeader.split(PARAMS_SPLIT_REGEXP))
                .filter(p -> p.contains(PARAMS_KEY_VALUE_SEPARATOR))
                .forEach(p -> {
                    String[] ps = p.split(PARAMS_KEY_VALUE_SEPARATOR);
                    String paramName = ps[0];
                    String paramValue = ps[1];
                    Optional<MigrationParameter> maybeMigrationParameter = Arrays.stream(MigrationParameter.values())
                            .filter(mp -> mp.getName().equals(paramName))
                            .findFirst();
                    if (maybeMigrationParameter.isPresent()) {
                        var migrationParameter = maybeMigrationParameter.get();
                        params.remove(migrationParameter);
                        params.put(migrationParameter, migrationParameter.parseValue(paramValue));
                    } else if (paramName.startsWith(USER_DEFINED_PARAM_NAME_PREFIX)) {
                        userDefinedParams.put(paramName, paramValue);
                    } else {
                        throw new IllegalArgumentException("Unknown parameter: " + paramName);
                    }
                });

        LOGGER.debug("Parsed header: " +
                                "title={} " +
                                "author={} " +
                                "params={} " +
                                "userDefinedParams={}",
                    title, author, params, userDefinedParams);

        migrationBuilder.header(title, author, params, userDefinedParams);
    }

    private String getTitle(String header) {
        var title = header.split(PARAMS_KEY_VALUE_SEPARATOR)[1].split(PARAMS_SPLIT_REGEXP)[0];
        LOGGER.debug("Got title {} from header {}", title, header);
        return title;
    }

    private String getAuthor(String header) {
        var author = header.split(PARAMS_KEY_VALUE_SEPARATOR)[0].split(PARAMS_SPLIT_REGEXP)[1];
        LOGGER.debug("Got author {} from header {}", author, header);
        return author;
    }

    private void validateMigrationFileHeader(String header) {
        header = header == null ? null : header.strip();
        if (!MIGRATION_FILE_HEADER.equals(header)) {
            throw new InvalidFileFormatException("Migration file must have header {}", MIGRATION_FILE_HEADER);
        }
    }

}
