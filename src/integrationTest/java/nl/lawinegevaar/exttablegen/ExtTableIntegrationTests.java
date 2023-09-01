// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.RFC4180Parser;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.type.*;
import nl.lawinegevaar.exttablegen.xmlconfig.ExtTableGenConfig;
import nl.lawinegevaar.exttablegen.xmlconfig.InformationalType;
import org.firebirdsql.management.FBManager;
import org.firebirdsql.util.FirebirdSupportInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.Logger.Level.WARNING;
import static java.util.Objects.requireNonNull;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.col;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.date;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.decimal;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.integralNumber;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.numeric;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.time;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.timestamp;
import static nl.lawinegevaar.exttablegen.IntegrationTestProperties.externalTableFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(RequireIntegrationTestConfigurationCondition.class)
class ExtTableIntegrationTests {

    private static final String TEST_DATA_RESOURCE_ROOT = "/integration-testdata/";
    private static final String CUSTOMERS_1000_PREFIX = "customers-1000";
    private static final String CUSTOMERS_TABLE_NAME = "CUSTOMERS";
    private static final String ID_VALUES_PREFIX = "id-values";
    private static final String ID_VALUES_DEC_PREFIX = ID_VALUES_PREFIX + "-dec";
    private static final String ID_VALUES_HEX_PREFIX = ID_VALUES_PREFIX + "-hex";
    private static final int ID_VALUES_ROW_COUNT = 18;
    private static final String DATE_VALUES_PREFIX = "date-values";
    private static final String DATE_VALUES_BASELINE_PREFIX = DATE_VALUES_PREFIX + "-baseline";
    private static final int DATE_VALUES_ROW_COUNT = 5;
    private static final String TIME_VALUES_PREFIX = "time-values";
    private static final String TIME_VALUES_BASELINE_PREFIX = TIME_VALUES_PREFIX + "-baseline";
    private static final int TIME_VALUES_ROW_COUNT = 7;
    private static final String TIMESTAMP_VALUES_PREFIX = "timestamp-values";
    private static final String TIMESTAMP_VALUES_BASELINE_PREFIX = TIMESTAMP_VALUES_PREFIX + "-baseline";
    private static final int TIMESTAMP_VALUES_ROW_COUNT = 7;
    private static final String FIXED_POINT_VALUES_BASELINE_PREFIX = "fixed-point-values-baseline";
    private static final int FIXED_POINT_VALUES_ROW_COUNT = 7;

    private static FBManager fbManager;
    private static final Path databasePath = IntegrationTestProperties.databasePath("integration-test.fdb");
    private final ConfigMapper configMapper = new ConfigMapper();

    @TempDir
    static Path forAllTempDir;
    @TempDir
    Path forEachTempDir;
    private static FirebirdSupportInfo firebirdSupportInfo;
    private static Path customers1000CsvFile;
    private static Path idValueDecCsvFile;
    private static Path idValueHexCsvFile;
    private static Path dateValuesBaselineCsvFile;
    private static Path timeValuesBaselineCsvFile;
    private static Path timestampValuesBaselineCsvFile;
    private static Path fixedPointValuesBaselineCsvFile;
    private final List<Path> filesToDelete = new ArrayList<>();

    @BeforeAll
    static void setupDb() throws Exception {
        fbManager = IntegrationTestProperties.createFBManager();
        fbManager.setFileName(databasePath.toString());
        fbManager.setCreateOnStart(true);
        fbManager.setDropOnStop(true);
        fbManager.start();
        try (Connection connection = IntegrationTestProperties.createConnection(databasePath)) {
            firebirdSupportInfo = FirebirdSupportInfo.supportInfoFor(connection);
        }
    }

    @BeforeAll
    static void copyTestDataFromResources() throws Exception {
        customers1000CsvFile = copyForAllResource(
                testDataResource(csvFilename(CUSTOMERS_1000_PREFIX)), csvFilename(CUSTOMERS_1000_PREFIX));
        idValueDecCsvFile = copyForAllResource(
                testDataResource(csvFilename(ID_VALUES_DEC_PREFIX)), csvFilename(ID_VALUES_DEC_PREFIX));
        idValueHexCsvFile = copyForAllResource(
                testDataResource(csvFilename(ID_VALUES_HEX_PREFIX)), csvFilename(ID_VALUES_HEX_PREFIX));
        dateValuesBaselineCsvFile = copyForAllResource(
                testDataResource(csvFilename(DATE_VALUES_BASELINE_PREFIX)), csvFilename(DATE_VALUES_BASELINE_PREFIX));
        timeValuesBaselineCsvFile = copyForAllResource(
                testDataResource(csvFilename(TIME_VALUES_BASELINE_PREFIX)), csvFilename(TIME_VALUES_BASELINE_PREFIX));
        timestampValuesBaselineCsvFile = copyForAllResource(
                testDataResource(csvFilename(TIMESTAMP_VALUES_BASELINE_PREFIX)),
                csvFilename(TIMESTAMP_VALUES_BASELINE_PREFIX));
        fixedPointValuesBaselineCsvFile = copyForAllResource(
                testDataResource(csvFilename(FIXED_POINT_VALUES_BASELINE_PREFIX)),
                csvFilename(FIXED_POINT_VALUES_BASELINE_PREFIX));
    }

    @AfterAll
    static void tearDownDb() throws Exception {
        try {
            fbManager.stop();
        } finally {
            fbManager = null;
        }
    }

    @AfterEach
    void cleanupFiles() {
        filesToDelete.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.getLogger(ExtTableIntegrationTests.class.getName())
                        .log(WARNING, "Exception cleaning up file " + path, e);
            }
        });
    }

    /**
     * Registers a file path for clean up after the test completes.
     *
     * @param path
     *         file path
     * @return {@code path} for convenience
     */
    private Path registerForCleanup(Path path) {
        filesToDelete.add(path);
        return path;
    }

    @ParameterizedTest
    @EnumSource(EndColumn.Type.class)
    void compareOriginalDataWithDataFromFirebird(EndColumn.Type endColumnType) throws Throwable {
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(CUSTOMERS_1000_PREFIX)));
        Path configFile = forEachTempDir.resolve(configFilename(CUSTOMERS_1000_PREFIX));
        createExternalTableFile(CUSTOMERS_TABLE_NAME, customers1000CsvFile, tableFile, endColumnType, configFile);

        assertExternalTable(configFile, CUSTOMERS_TABLE_NAME, customers1000CsvFile, 1000, endColumnType);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            configName,                           columnToCheck, expectedJdbcType
            customers-1000-index-smallint.xml,    1,             SMALLINT
            customers-1000-index-integer.xml,     1,             INTEGER
            customers-1000-index-bigint.xml,      1,             BIGINT
            customers-1000-index-int128.xml,      1,             NUMERIC
            customers-1000-index-decimal_9_0.xml, 1,             DECIMAL
            customers-1000-index-numeric_9_0.xml, 1,             NUMERIC
            customers-1000-index-date.xml,        11,            DATE
            """)
    void testCustomersCustomTypes_simple(String configName, int columnToCheck, JDBCType expectedJdbcType)
            throws Throwable {
        if (expectedJdbcType == JDBCType.NUMERIC && configName.contains("int128")) {
            assumeTrue(firebirdSupportInfo.supportsInt128(), "Test requires INT128 support");
        }
        Path configIn = copyForEachResource(testDataResource(configName), configName);
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(CUSTOMERS_1000_PREFIX)));
        Path configOutFile = forEachTempDir.resolve(configFilename(CUSTOMERS_1000_PREFIX));
        createExternalTableFileFromExistingConfig(configIn, CUSTOMERS_TABLE_NAME,
                customers1000CsvFile, tableFile, configOutFile);

        assertExternalTable(configOutFile, CUSTOMERS_TABLE_NAME, customers1000CsvFile, 1000, EndColumn.Type.NONE,
                rsmd -> assertEquals(expectedJdbcType.getVendorTypeNumber(), rsmd.getColumnType(columnToCheck)));
    }

    @Test
    void integralNumberIntegrationTest_boundaries() throws Throwable {
        Path csvFile = forEachTempDir.resolve("integral-boundary.csv");
        Path tableFile = registerForCleanup(externalTableFile("integral-boundary.dat"));
        Files.writeString(csvFile,
                """
                name,smallint,integer,bigint,int128
                minimum,-32768,-2147483648,-9223372036854775808,-170141183460469231731687303715884105728
                minus one,-1,-1,-1,-1
                zero,0,0,0,0
                plus one,1,1,1,1
                maximum,32767,2147483647,9223372036854775807,170141183460469231731687303715884105727
                """);
        String tableName = "INTEGRAL_BOUNDARY";
        Path configFile = forEachTempDir.resolve("integral-boundary.xml");
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(tableName,
                            List.of(new Column("name", new FbChar(15, FbEncoding.ASCII)),
                                    new Column("smallint", new FbSmallint()),
                                    new Column("integer", new FbInteger()),
                                    new Column("bigint", new FbBigint()),
                                    new Column("int128", firebirdSupportInfo.supportsInt128()
                                            ? new FbInt128() : new FbChar(40, FbEncoding.ASCII))),
                            tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, tableName, csvFile, tableFile, configFile);

        assertExternalTable(configFile, tableName, csvFile, 5, EndColumn.Type.NONE);
    }

    // TODO Consider pruning testcases to reduce runtime
    @ParameterizedTest
    @CsvFileSource(resources = "/integration-testcases/verify-alignment-testcases.csv", useHeadersInDisplayName = true)
    void verifyAlignment(String firstType, String secondType, String thirdType, int totalColumnCount) throws Throwable {
        if (Arrays.asList(firstType, secondType, thirdType).contains("int128")) {
            assumeTrue(firebirdSupportInfo.supportsInt128(), "Test requires INT128 support");
        }
        var columns = new ArrayList<Column>(Math.max(3, totalColumnCount));
        columns.add(createColumn("COLUMN_1", firstType));
        columns.add(createColumn("COLUMN_2", secondType));
        columns.add(createColumn("COLUMN_3", thirdType));
        for (int idx = 4; idx <= totalColumnCount; idx++) {
            columns.add(ColumnFixtures.smallint("COLUMN_" + idx, null));
        }
        Path csvFile = forEachTempDir.resolve("verify-alignment.csv");
        int rowCount = 5;
        generateCsvFile(csvFile, columns, rowCount);
        Path tableFile = registerForCleanup(externalTableFile("verify-alignment.dat"));
        String tableName = "VERIFY_ALIGNMENT";
        Path configFile = forEachTempDir.resolve("verify-alignment.xml");
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(tableName, columns, tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, tableName, csvFile, tableFile, configFile);

        assertExternalTable(configFile, tableName, csvFile, rowCount, EndColumn.Type.NONE);
    }

    @Test
    void testWithExplicitConverter_fromFile() throws Throwable {
        Path configFile = copyForEachResource(testDataResource(configFilename(ID_VALUES_HEX_PREFIX + "-integer")),
                configFilename(ID_VALUES_HEX_PREFIX + "-integer"));
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(ID_VALUES_PREFIX)));
        createExternalTableFileFromExistingConfig(configFile, ID_VALUES_PREFIX, idValueHexCsvFile, tableFile, configFile);

        // NOTE: We're using the "dec" value for comparison, because we compare as decimal values
        assertExternalTable(configFile, ID_VALUES_PREFIX, idValueDecCsvFile, ID_VALUES_ROW_COUNT, EndColumn.Type.NONE);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            type,     radix
            bigint,   10
            bigint,   16
            int128,   10
            int128,   16
            integer,  10
            integer,  16
            smallint, 10
            smallint, 16
            """)
    void testWithExplicitConverter_parseIntegral(String typeName, int radix) throws Throwable {
        if ("int128".equals(typeName)) {
            assumeTrue(firebirdSupportInfo.supportsInt128(), "Test requires INT128 support");
        }
        assert radix == 10 || radix == 16 : "Test only works for radix 10 or 16 (due to available test data)";
        Path csvFile = radix == 10 ? idValueDecCsvFile : idValueHexCsvFile;
        var columns = List.of(integralNumber("Id", typeName, Converter.parseIntegralNumber(typeName, radix)));
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(ID_VALUES_PREFIX)));
        Path configFile = forEachTempDir.resolve(configFilename(ID_VALUES_PREFIX));
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(ID_VALUES_PREFIX, columns, tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, ID_VALUES_PREFIX, csvFile, tableFile, configFile);

        // NOTE: We're using the "dec" value for comparison, because we compare as decimal values
        assertExternalTable(configFile, ID_VALUES_PREFIX, idValueDecCsvFile, ID_VALUES_ROW_COUNT, EndColumn.Type.NONE);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            pattern,     locale
            dd-MM-yyyy,
            d MMMM yyyy, nl-NL
            d MMMM yyyy, en
            """)
    void testWithExplicitConverter_parseDatetime_date(String pattern, String locale) throws Throwable {
        String testName = DATE_VALUES_PREFIX + '-' + pattern.replace(' ', '_') + (locale != null ? '-' + locale : "");
        String testCsvFilename = csvFilename(testName);
        Path csvFile = copyForEachResource(testDataResource(testCsvFilename), testCsvFilename);
        var columns = List.of(date("Date", Converter.parseDatetime(pattern, locale)));
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(testName)));
        Path configFile = forEachTempDir.resolve(configFilename(testName));
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(DATE_VALUES_PREFIX, columns, tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, DATE_VALUES_PREFIX, csvFile, tableFile, configFile);

        assertExternalTable(configFile, DATE_VALUES_PREFIX, dateValuesBaselineCsvFile, DATE_VALUES_ROW_COUNT,
                EndColumn.Type.NONE);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            pattern,        locale
            # NOTE: This pattern has 5 fractional digits, while Firebird only supports 4, the file populates the fifth
            # digit with 1 to show it is ignored
            HH.mm.ss.SSSSS,
            h:mm:ss.SSSS a, en-US
            """)
    void testWithExplicitConverter_parseDatetime_time(String pattern, String locale) throws Throwable {
        String testName = TIME_VALUES_PREFIX + '-' + pattern.replace(' ', '_').replace(':', '_')
                          + (locale != null ? '-' + locale : "");
        String testCsvFilename = csvFilename(testName);
        Path csvFile = copyForEachResource(testDataResource(testCsvFilename), testCsvFilename);
        var columns = List.of(time("Time", Converter.parseDatetime(pattern, locale)));
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(testName)));
        Path configFile = forEachTempDir.resolve(configFilename(testName));
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(TIME_VALUES_PREFIX, columns, tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, TIME_VALUES_PREFIX, csvFile, tableFile, configFile);

        assertExternalTable(configFile, TIME_VALUES_PREFIX, timeValuesBaselineCsvFile, TIME_VALUES_ROW_COUNT,
                EndColumn.Type.NONE);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            pattern,                    locale
            SQL_TIMESTAMP,
            dd-MM-yyyy[ HH:mm:ss.SSSS],
            d MMMM yyyy h:mm:ss.SSSS a, en-US
            """)
    void testWithExplicitConverter_parseDatetime_timestamp(String pattern, String locale) throws Throwable {
        String testName = TIMESTAMP_VALUES_PREFIX + '-' + pattern.replace(' ', '_').replace(':', '_')
                          + (locale != null ? '-' + locale : "");
        String testCsvFilename = csvFilename(testName);
        Path csvFile = copyForEachResource(testDataResource(testCsvFilename), testCsvFilename);
        var columns = List.of(timestamp("Time", Converter.parseDatetime(pattern, locale)));
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(testName)));
        Path configFile = forEachTempDir.resolve(configFilename(testName));
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(TIMESTAMP_VALUES_PREFIX, columns, tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, TIMESTAMP_VALUES_PREFIX, csvFile, tableFile, configFile);

        assertExternalTable(configFile, TIMESTAMP_VALUES_PREFIX, timestampValuesBaselineCsvFile,
                TIMESTAMP_VALUES_ROW_COUNT, EndColumn.Type.NONE);
    }

    @Test
    void fixedPointIntegrationTest_boundaries() throws Throwable {
        Path csvFile = forEachTempDir.resolve("fixed-point-boundary.csv");
        Path tableFile = registerForCleanup(externalTableFile("fixed-point-boundary.dat"));
        Files.writeString(csvFile,
                """
                name,numeric_4_2,numeric_9_2,numeric_18_2,numeric_38_2,decimal_9_2,decimal_18_2,decimal_38_2
                minimum,-327.68,-21474836.48,-92233720368547758.08,-1701411834604692317316873037158841057.28,-21474836.48,-92233720368547758.08,-1701411834604692317316873037158841057.28
                minus one,-1.00,-1.00,-1.00,-1.00,-1.00,-1.00,-1.00
                minus 1/100,-0.01,-0.01,-0.01,-0.01,-0.01,-0.01,-0.01
                zero,0.00,0.00,0.00,0.00,0.00,0.00,0.00
                plus 1/100,0.01,0.01,0.01,0.01,0.01,0.01,0.01
                plus one,1.00,1.00,1.00,1.00,1.00,1.00,1.00
                maximum,327.67,21474836.47,92233720368547758.07,1701411834604692317316873037158841057.27,21474836.47,92233720368547758.07,1701411834604692317316873037158841057.27
                """);
        String tableName = "FIXED_POINT_BOUNDARY";
        Path configFile = forEachTempDir.resolve("fixed-point-boundary.xml");
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(tableName,
                            List.of(new Column("name", new FbChar(15, FbEncoding.ASCII)),
                                    new Column("numeric_4_2", new FbNumeric(4, 2, null)),
                                    new Column("numeric_9_2", new FbNumeric(9, 2, null)),
                                    new Column("numeric_18_2", new FbNumeric(18, 2, null)),
                                    new Column("numeric_38_2", firebirdSupportInfo.supportsDecimalPrecision(38)
                                            ? new FbNumeric(38, 2, null) : new FbChar(41, FbEncoding.ASCII)),
                                    new Column("decimal_9_2", new FbDecimal(9, 2, null)),
                                    new Column("decimal_18_2", new FbDecimal(18, 2, null)),
                                    new Column("decimal_38_2", firebirdSupportInfo.supportsDecimalPrecision(38)
                                            ? new FbDecimal(38, 2, null) : new FbChar(41, FbEncoding.ASCII))),
                            tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, tableName, csvFile, tableFile, configFile);

        assertExternalTable(configFile, tableName, csvFile, 7, EndColumn.Type.NONE);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            type,    locale
            numeric, en-US
            numeric, nl-NL
            numeric, en-US
            decimal, nl-NL
            """)
    void testWithExplicitConverter_parseBigDecimal(String type, String locale) throws Throwable {
        String prefix = type + "-values";
        String testName = prefix + '-' + locale;
        String testCsvFilename = csvFilename(testName);
        Path csvFile = copyForEachResource(testDataResource(testCsvFilename), testCsvFilename);
        var converter = Converter.parseBigDecimal(locale);
        Column column = switch (type) {
            case "numeric" -> numeric("Value", 9, 2, null, converter);
            case "decimal" -> decimal("Value", 9, 2, null, converter);
            default -> throw new AssertionError("Unsupported type: " + type);
        };
        var columns = List.of(column);
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(testName)));
        Path configFile = forEachTempDir.resolve(configFilename(testName));
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(prefix, columns, tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, prefix, csvFile, tableFile, configFile);

        assertExternalTable(configFile, prefix, fixedPointValuesBaselineCsvFile,
                FIXED_POINT_VALUES_ROW_COUNT, EndColumn.Type.NONE);
    }

    @Test
    void testWithCustomCsvParser() throws Throwable {
        Path csvFile = forEachTempDir.resolve("custom-csv-format.csv");
        Path tableFile = registerForCleanup(externalTableFile("custom-csv-format.dat"));
        Files.writeString(csvFile,
                """
                'COLUMN_1'\t'COLUMN_2'
                VAL_1_1\tVAL_1_2
                'VAL_2_1'\t'VAL_2_2'
                'VAL_3#'_1'\t'VAL"3_2'
                """);
        var parserConfig = CsvParserConfig.custom(CharValue.of("APOS"), CharValue.of("TAB"), CharValue.of('#'), true,
                false, false);
        String tableName = "CUSTOM_CSV_FORMAT";
        Path configFile = forEachTempDir.resolve("custom-csv-format.xml");

        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    createTableConfig(tableName,
                            List.of(new Column("COLUMN_1", new FbChar(15, FbEncoding.ASCII)),
                                    new Column("COLUMN_2", new FbChar(15, FbEncoding.ASCII))),
                            tableFile),
                    TableDerivationConfig.getDefault(),
                    createCsvFileConfig(csvFile).withParserConfig(parserConfig));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, tableName, csvFile, tableFile, configFile);

        assertExternalTable(configFile, tableName, csvFile, parserConfig, 3, EndColumn.Type.NONE);
    }

    // TODO Maybe some or all of the following methods should be moved to test-common

    private static CsvFileConfig createCsvFileConfig(Path csvFile) {
        return new CsvFileConfig(csvFile, StandardCharsets.UTF_8, true, CsvParserConfig.of());
    }

    private static TableConfig createTableConfig(String tableName, List<Column> columns, Path tableFile) {
        return new TableConfig(tableName, columns, new TableFile(tableFile, false), ByteOrderType.AUTO);
    }

    private static Column createColumn(String name, String columnType) {
        class Holder {
            static final Pattern CHAR_PATTERN = Pattern.compile("char_(\\d+)");
            static final Pattern FIXED_POINT_PATTERN = Pattern.compile("(decimal|numeric)_(\\d+)_(\\d+)");
        }
        Matcher charMatcher = Holder.CHAR_PATTERN.matcher(columnType);
        if (charMatcher.matches()) {
            return col(name, Integer.parseInt(charMatcher.group(1)));
        }
        Matcher fixedPointMatcher = Holder.FIXED_POINT_PATTERN.matcher(columnType);
        if (fixedPointMatcher.matches()) {
            String type = fixedPointMatcher.group(1);
            int precision = Integer.parseInt(fixedPointMatcher.group(2));
            int scale = Integer.parseInt(fixedPointMatcher.group(3));
            return switch (type) {
                case "decimal" -> decimal(name, precision, scale);
                case "numeric" -> numeric(name, precision, scale);
                default -> throw new AssertionError("invalid type: " + type);
            };
        }
        return switch (columnType) {
            case "date" -> date(name, null);
            case "time" -> time(name, null);
            case "timestamp" -> timestamp(name, null);
            default -> integralNumber(name, columnType);
        };
    }

    private static void generateCsvFile(Path csvFile, List<Column> columns, int rowCount) throws IOException {
        assert rowCount > 1 : "Generate a CSV file with two or more rows, a single row can hide some issues";
        try (var out = Files.newBufferedWriter(csvFile);
             ICSVWriter csvWriter = new CSVWriterBuilder(out)
                     .withParser(new RFC4180Parser())
                     .build()) {
            // write header
            csvWriter.writeNext(columns.stream().map(Column::name).toArray(String[]::new));
            // write row data
            for (int row = 1; row <= rowCount; row++) {
                csvWriter.writeNext(generateRow(row, columns));
            }
        }
    }

    // NOTE The implementation of this method is coupled to the requirements of verifyAlignment; it might not be
    // generally usable
    private static String[] generateRow(int row, List<Column> columns) {
        int columnCount = columns.size();
        String[] rowData = new String[columnCount];
        for (int colIdx = 0; colIdx < columnCount; colIdx++) {
            rowData[colIdx] = generateColumnData(row, colIdx + 1, columns.get(colIdx).datatype());
        }
        return rowData;
    }

    private static String generateColumnData(int row, int colIdx, FbDatatype<?> datatype) {
        if (datatype instanceof FbChar fbChar) {
            String stringValue = Integer.toString(row * colIdx, 36);
            int requiredLength = fbChar.length();
            int actualLength = stringValue.length();
            if (actualLength > requiredLength) {
                stringValue = stringValue.substring(actualLength - requiredLength);
            } else if (actualLength < requiredLength) {
                stringValue = "_".repeat(requiredLength - actualLength) + stringValue;
            }
            return stringValue;
        }
        // We use a negative value, so at least for starting rows the sign bit is set, which makes it easier to detect
        // alignment issues
        long value = -1L * row * colIdx;
        String typeName = datatype.getClass().getSimpleName();
        return switch (typeName) {
            case "FbSmallint" -> Short.toString((short) value);
            case "FbInteger" -> Integer.toString((int) value);
            case "FbBigint", "FbInt128" -> Long.toString(value);
            case "FbDate" -> LocalDate.now().plusDays(value).toString();
            case "FbTime" -> LocalTime.now().plusSeconds(value).truncatedTo(FbTime.FB_TIME_UNIT).toString();
            case "FbTimestamp" -> LocalDateTime.now().plusDays(Math.abs(value)).plusSeconds(value)
                    .truncatedTo(FbTime.FB_TIME_UNIT).toString();
            case "FbNumeric", "FbDecimal" -> {
                FbFixedPointDatatype fixedPoint = (FbFixedPointDatatype) datatype;
                if (typeName.equals("FbNumeric") && fixedPoint.precision() <= 4) {
                    value = (short) value;
                } else if (fixedPoint.precision() <= 9) {
                    value = (int) value;
                }
                yield new BigDecimal(BigInteger.valueOf(value), fixedPoint.scale()).toPlainString();
            }
            default -> throw new IllegalArgumentException("Unsupported datatype: " + typeName);
        };
    }

    private static void createExternalTableFile(String tableName, Path csvFile, Path tableFile,
            EndColumn.Type endColumnType, Path configOut) {
        int exitCode = ExtTableGenMain.parseAndExecute(
                "--csv-file", csvFile.toString(),
                "--table-name", tableName,
                "--table-file", tableFile.toString(),
                "--overwrite-table-file",
                "--end-column", endColumnType.name(),
                "--config-out", configOut.toString(),
                "--overwrite-config");
        assertEquals(0, exitCode, "expected zero exit-code for successful execution");
    }

    private static void createExternalTableFileFromExistingConfig(Path configIn, String tableName, Path csvFile,
            Path tableFile, Path configOut) {
        int exitCode = ExtTableGenMain.parseAndExecute(
                "--config-in", configIn.toString(),
                "--table-name", tableName,
                "--csv-file", csvFile.toString(),
                "--table-file", tableFile.toString(),
                "--overwrite-table-file",
                "--config-out", configOut.toString(),
                "--overwrite-config");
        assertEquals(0, exitCode, "expected zero exit-code for successful execution");
    }

    private String getDdl(Path configPath) throws Exception {
        try (var in = Files.newInputStream(configPath)) {
            ExtTableGenConfig extTableGenConfig = configMapper.readAsExtTableGenConfig(in);
            InformationalType informational = requireNonNull(extTableGenConfig.getInformational(), "informational");
            return requireNonNull(informational.getDdl(), "informational/ddl")
                    .replaceFirst("(?i)^\\s*create table", "recreate table");
        }
    }

    private void assertExternalTable(Path configFile, String tableName, Path expectedDataCsvFile, int expectedRowCount,
            EndColumn.Type expectedEndColumnType) throws Throwable {
        assertExternalTable(configFile, tableName, expectedDataCsvFile, expectedRowCount, expectedEndColumnType,
                rsmd -> {});
    }

    private void assertExternalTable(Path configFile, String tableName, Path expectedDataCsvFile,
            CsvParserConfig csvParserConfig, int expectedRowCount, EndColumn.Type expectedEndColumnType)
            throws Throwable {
        assertExternalTable(configFile, tableName, expectedDataCsvFile, csvParserConfig, expectedRowCount,
                expectedEndColumnType, rsmd -> {});
    }

    private void assertExternalTable(Path configFile, String tableName, Path expectedDataCsvFile, int expectedRowCount,
            EndColumn.Type expectedEndColumnType, ThrowingConsumer<ResultSetMetaData> resultSetMetaDataAssertions)
            throws Throwable {
        assertExternalTable(configFile, tableName, expectedDataCsvFile, CsvParserConfig.of(), expectedRowCount,
                expectedEndColumnType, resultSetMetaDataAssertions);
    }

    private void assertExternalTable(Path configFile, String tableName, Path expectedDataCsvFile,
            CsvParserConfig csvParserConfig, int expectedRowCount, EndColumn.Type expectedEndColumnType,
            ThrowingConsumer<ResultSetMetaData> resultSetMetaDataAssertions) throws Throwable {
        String ddl = getDdl(configFile);
        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(tableName, true))) {
                resultSetMetaDataAssertions.accept(rs.getMetaData());
                // NOTE: We're using the "dec" value for comparison, because we compare as decimal values
                assertResultSet(expectedDataCsvFile, csvParserConfig, expectedRowCount, rs, expectedEndColumnType);
            }
        }
    }

    private void assertResultSet(Path expectedDataCsvFile, CsvParserConfig csvParserConfig, int expectedRowCount,
            ResultSet rs, EndColumn.Type endColumnType) throws Exception {
        final int rsColumnCount = rs.getMetaData().getColumnCount();
        final int expectedCsvColumnCount = endColumnType == EndColumn.Type.NONE ? rsColumnCount : rsColumnCount - 1;
        final String expectedEndColumnValue = switch (endColumnType) {
            case LF -> "\n";
            case CRLF -> "\r\n";
            case NONE -> null;
        };

        var csvFileDriver = new CsvFile(InputResource.of(expectedDataCsvFile),
                new CsvFile.Config(StandardCharsets.UTF_8, 0, true, csvParserConfig));
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            var resultSetVsCsvValidator = new AbstractRowProcessor() {

                int count = 0;

                @Override
                public ProcessingResult onRow(Row row) {
                    count++;
                    try {
                        assertTrue(rs.next(), "expected a result set row for received CSV file row " + row.line());
                        assertEquals(expectedCsvColumnCount, row.size(),
                                "column count mismatch between result set row and CSV file row " + row.line());
                        for (int idx = 1; idx <= expectedCsvColumnCount; idx++) {
                            String csvValue = row.get(idx - 1).trim();
                            switch (rsmd.getColumnType(idx)) {
                            case Types.TIME -> {
                                // Handling of TIME defaults to using java.sql.Time, which doesn't have sub-second
                                // precision and using LocalTime.toString() results in issues with trailing zeroes
                                LocalTime dbValue = rs.getObject(idx, LocalTime.class);
                                assertEquals(LocalTime.parse(csvValue), dbValue,
                                        "expected equal values for CSV file row %d, column %d"
                                                .formatted(row.line(), idx));
                            }
                            case Types.TIMESTAMP -> {
                                // java.sql.Timestamp.toString() result in issues with trailing zeroes
                                LocalDateTime dbValue = rs.getObject(idx, LocalDateTime.class);
                                assertEquals(LocalDateTime.parse(csvValue), dbValue,
                                        "expected equal values for CSV file row %d, column %d"
                                                .formatted(row.line(), idx));
                            }
                            default -> {
                                String dbValue = rs.getString(idx).trim();
                                assertEquals(csvValue, dbValue,
                                        "expected equal trimmed values for CSV file row %d, column %d"
                                                .formatted(row.line(), idx));
                            }
                            }
                        }
                        if (expectedEndColumnValue != null) {
                            assertEquals(expectedEndColumnValue, rs.getString(rsColumnCount),
                                    "unexpected end column value for CSV file row " + row.line());
                        }
                        return ProcessingResult.continueProcessing();
                    } catch (SQLException e) {
                        throw new AssertionFailedError("Unexpected SQLException for row " + row, e);
                    }
                }
            };
            ProcessingResult processingResult = csvFileDriver.readFile(resultSetVsCsvValidator);
            assertInstanceOf(ProcessingResult.Done.class, processingResult, "Unexpected processing result");
            assertFalse(rs.next(), "Result set has more rows than CSV file");
            assertEquals(expectedRowCount, resultSetVsCsvValidator.count, "Unexpected CSV row count");
        } catch (FatalRowProcessingException e) {
            if (e.getCause() instanceof AssertionFailedError afe) {
                throw afe;
            }
            throw e;
        }
    }

    private static Path copyForAllResource(String resourceName, String filename) throws IOException {
        try (InputStream in = ExtTableIntegrationTests.class.getResourceAsStream(resourceName)) {
            if (in == null) throw new FileNotFoundException("Could not find resource " + resourceName);
            Path destinationPath = forAllTempDir.resolve(filename);
            Files.copy(in, destinationPath);
            return destinationPath;
        }
    }

    private Path copyForEachResource(String resourceName, String filename) throws IOException {
        try (InputStream in = ExtTableIntegrationTests.class.getResourceAsStream(resourceName)) {
            if (in == null) throw new FileNotFoundException("Could not find resource " + resourceName);
            Path destinationPath = forEachTempDir.resolve(filename);
            Files.copy(in, destinationPath);
            return destinationPath;
        }
    }

    private static String testDataResource(String filename) {
        return TEST_DATA_RESOURCE_ROOT + filename;
    }

    private static String csvFilename(String prefix) {
        return prefix + ".csv";
    }

    private static String tableFilename(String prefix) {
        return prefix + ".dat";
    }

    private static String configFilename(String prefix) {
        return prefix + ".xml";
    }

}
