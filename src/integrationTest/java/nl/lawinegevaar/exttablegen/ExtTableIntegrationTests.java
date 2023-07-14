// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.RFC4180Parser;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.xmlconfig.ExtTableGenConfig;
import nl.lawinegevaar.exttablegen.xmlconfig.InformationalType;
import org.firebirdsql.management.FBManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.Logger.Level.WARNING;
import static java.util.Objects.requireNonNull;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.integralNumber;
import static nl.lawinegevaar.exttablegen.IntegrationTestProperties.externalTableFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(RequireIntegrationTestConfigurationCondition.class)
class ExtTableIntegrationTests {

    private static final String TEST_DATA_RESOURCE_ROOT = "/integration-testdata/";
    private static final String CUSTOMERS_1000_PREFIX = "customers-1000";
    private static final String CUSTOMERS_TABLE_NAME = "CUSTOMERS";
    private static final String CUSTOMERS_1000_RESOURCE = TEST_DATA_RESOURCE_ROOT + csvFilename(CUSTOMERS_1000_PREFIX);
    private static final String ID_VALUES_PREFIX = "id-values";
    private static final String ID_VALUES_DEC_PREFIX = ID_VALUES_PREFIX + "-dec";
    private static final String ID_VALUES_DEC_RESOURCE = TEST_DATA_RESOURCE_ROOT + csvFilename(ID_VALUES_DEC_PREFIX);
    private static final String ID_VALUES_HEX_PREFIX = ID_VALUES_PREFIX + "-hex";
    private static final String ID_VALUES_HEX_RESOURCE = TEST_DATA_RESOURCE_ROOT + csvFilename(ID_VALUES_HEX_PREFIX);
    private static final int ID_VALUES_ROW_COUNT = 18;

    private static FBManager fbManager;
    private static final Path databasePath = IntegrationTestProperties.databasePath("integration-test.fdb");
    private final ConfigMapper configMapper = new ConfigMapper();

    @TempDir
    static Path forAllTempDir;
    @TempDir
    Path forEachTempDir;
    private static Path customers1000CsvFile;
    private static Path idValueDecCsvFile;
    private static Path idValueHexCsvFile;
    private final List<Path> filesToDelete = new ArrayList<>();

    @BeforeAll
    static void setupDb() throws Exception {
        fbManager = IntegrationTestProperties.createFBManager();
        fbManager.setFileName(databasePath.toString());
        fbManager.setCreateOnStart(true);
        fbManager.setDropOnStop(true);
        fbManager.start();
    }

    @BeforeAll
    static void copyTestDataFromResources() throws Exception {
        customers1000CsvFile = copyForAllResource(CUSTOMERS_1000_RESOURCE, csvFilename(CUSTOMERS_1000_PREFIX));
        idValueDecCsvFile = copyForAllResource(ID_VALUES_DEC_RESOURCE, csvFilename(ID_VALUES_DEC_PREFIX));
        idValueHexCsvFile = copyForAllResource(ID_VALUES_HEX_RESOURCE, csvFilename(ID_VALUES_HEX_PREFIX));
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
    void compareOriginalDataWithDataFromFirebird(EndColumn.Type endColumnType) throws Exception {
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(CUSTOMERS_1000_PREFIX)));
        Path configFile = forEachTempDir.resolve(configFilename(CUSTOMERS_1000_PREFIX));
        createExternalTableFile(CUSTOMERS_TABLE_NAME, customers1000CsvFile, tableFile, endColumnType, configFile);
        String ddl = getDdl(configFile);

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(CUSTOMERS_TABLE_NAME, true))) {
                assertResultSet(customers1000CsvFile, 1000, rs, endColumnType);
            }
        }
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            configName,                        expectedJdbcType
            customers-1000-index-smallint.xml, SMALLINT
            customers-1000-index-integer.xml,  INTEGER
            customers-1000-index-bigint.xml,   BIGINT
            customers-1000-index-int128.xml,   NUMERIC
            """)
    void integralNumberIntegrationTest_simple(String configName, JDBCType expectedJdbcType) throws Exception {
        Path configIn = copyForEachResource(TEST_DATA_RESOURCE_ROOT + configName, configName);
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(CUSTOMERS_1000_PREFIX)));
        Path configOutFile = forEachTempDir.resolve(configFilename(CUSTOMERS_1000_PREFIX));
        createExternalTableFileFromExistingConfig(configIn, CUSTOMERS_TABLE_NAME,
                customers1000CsvFile, tableFile, configOutFile);
        String ddl = getDdl(configOutFile);

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(CUSTOMERS_TABLE_NAME, true))) {
                var rsmd = rs.getMetaData();
                assertEquals(expectedJdbcType.getVendorTypeNumber(), rsmd.getColumnType(1));
                assertResultSet(customers1000CsvFile, 1000, rs, EndColumn.Type.NONE);
            }
        }
    }

    @Test
    void integralNumberIntegrationTest_boundaries() throws Exception {
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
                    new TableConfig(tableName,
                            List.of(new Column("name", new FbChar(15, FbEncoding.ASCII)),
                                    new Column("smallint", new FbSmallint()),
                                    new Column("integer", new FbInteger()),
                                    new Column("bigint", new FbBigint()),
                                    new Column("int128", new FbInt128())),
                            new TableFile(tableFile, false), ByteOrderType.AUTO),
                    TableDerivationConfig.getDefault(),
                    new CsvFileConfig(csvFile, StandardCharsets.UTF_8, true));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, tableName, csvFile, tableFile, configFile);
        String ddl = getDdl(configFile);

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(tableName, true))) {
                assertResultSet(csvFile, 5, rs, EndColumn.Type.NONE);
            }
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/integration-testcases/verify-alignment-testcases.csv", useHeadersInDisplayName = true)
    void verifyAlignment(String firstType, String secondType, String thirdType, int totalColumnCount) throws Exception {
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
                    new TableConfig(tableName, columns, new TableFile(tableFile, false), ByteOrderType.AUTO),
                    TableDerivationConfig.getDefault(),
                    new CsvFileConfig(csvFile, StandardCharsets.UTF_8, true));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, tableName, csvFile, tableFile, configFile);
        String ddl = getDdl(configFile);

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(tableName, true))) {
                assertResultSet(csvFile, rowCount, rs, EndColumn.Type.NONE);
            }
        }
    }

    @Test
    void testWithExplicitConverter_fromFile() throws Exception {
        Path configFile = copyForEachResource(
                TEST_DATA_RESOURCE_ROOT + configFilename(ID_VALUES_HEX_PREFIX + "-integer"),
                configFilename(ID_VALUES_HEX_PREFIX + "-integer"));
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(ID_VALUES_PREFIX)));
        createExternalTableFileFromExistingConfig(configFile, ID_VALUES_PREFIX, idValueHexCsvFile, tableFile, configFile);
        String ddl = getDdl(configFile);

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(ID_VALUES_PREFIX, true))) {
                // NOTE: We're using the "dec" value for comparison, because we compare as decimal values
                assertResultSet(idValueDecCsvFile, ID_VALUES_ROW_COUNT, rs, EndColumn.Type.NONE);
            }
        }
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
    void testWithExplicitConverter_parseIntegral(String typeName, int radix) throws Exception {
        assert radix == 10 || radix == 16 : "Test only works for radix 10 or 16 (due to available test data)";
        Path csvFile = radix == 10 ? idValueDecCsvFile : idValueHexCsvFile;
        Converter<?> converter = Converter.parseIntegralNumber(typeName, radix);
        var columns = List.of(integralNumber("Id", typeName, converter));
        Path tableFile = registerForCleanup(externalTableFile(tableFilename(ID_VALUES_PREFIX)));
        Path configFile = forEachTempDir.resolve(configFilename(ID_VALUES_PREFIX));
        try (var out = Files.newOutputStream(configFile)) {
            var etgConfig = new EtgConfig(
                    new TableConfig(ID_VALUES_PREFIX, columns, new TableFile(tableFile, false), ByteOrderType.AUTO),
                    TableDerivationConfig.getDefault(),
                    new CsvFileConfig(csvFile, StandardCharsets.UTF_8, true));
            configMapper.write(etgConfig, out);
        }
        createExternalTableFileFromExistingConfig(configFile, ID_VALUES_PREFIX, csvFile, tableFile, configFile);
        String ddl = getDdl(configFile);

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(ID_VALUES_PREFIX, true))) {
                // NOTE: We're using the "dec" value for comparison, because we compare as decimal values
                assertResultSet(idValueDecCsvFile, ID_VALUES_ROW_COUNT, rs, EndColumn.Type.NONE);
            }
        }
    }

    // TODO Maybe some or all of the following methods should be moved to test-common

    private static Column createColumn(String name, String columnType) {
        class Holder {
            static final Pattern CHAR_PATTERN = Pattern.compile("char_(\\d+)");
        }
        Matcher charMatcher = Holder.CHAR_PATTERN.matcher(columnType);
        if (charMatcher.matches()) {
            return ColumnFixtures.col(name, Integer.parseInt(charMatcher.group(1)));
        }
        return integralNumber(name, columnType);
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
        if (datatype instanceof FbSmallint) {
            return Short.toString((short) value);
        } else if (datatype instanceof FbInteger) {
            return Integer.toString((int) value);
        } else if (datatype instanceof FbBigint || datatype instanceof FbInt128) {
            return Long.toString(value);
        } else {
            throw new IllegalArgumentException("Unsupported datatype: " + datatype.getClass().getSimpleName());
        }
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

    private void assertResultSet(Path csvFile, int expectedRowCount, ResultSet rs, EndColumn.Type endColumnType)
            throws Exception {
        final int rsColumnCount = rs.getMetaData().getColumnCount();
        final int expectedCsvColumnCount = endColumnType == EndColumn.Type.NONE ? rsColumnCount : rsColumnCount - 1;
        final String expectedEndColumnValue = switch (endColumnType) {
            case LF -> "\n";
            case CRLF -> "\r\n";
            case NONE -> null;
        };

        var csvFileDriver = new CsvFile(InputResource.of(csvFile), new CsvFile.Config(StandardCharsets.UTF_8, 0, true));
        try {
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
                            String dbValue = rs.getString(idx).trim();
                            assertEquals(csvValue, dbValue,
                                    "expected equal trimmed values for CSV file row %d, column %d".formatted(row.line(), idx));
                        }
                        if (expectedEndColumnValue != null) {
                            assertEquals(expectedEndColumnValue, rs.getString(rsColumnCount),
                                    "unexpected end column value for CSV file row " + row.line());
                        }
                        return ProcessingResult.continueProcessing();
                    } catch (SQLException e) {
                        throw new AssertionFailedError("Unexpected SQLException", e);
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
