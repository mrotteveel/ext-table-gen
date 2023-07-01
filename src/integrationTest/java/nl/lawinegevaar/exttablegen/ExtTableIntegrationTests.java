// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

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
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.Logger.Level.WARNING;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(RequireIntegrationTestConfigurationCondition.class)
class ExtTableIntegrationTests {

    private static final String TEST_DATA_RESOURCE_ROOT = "/integration-testdata/";
    private static final String CUSTOMERS_1000_PREFIX = "customers-1000";
    private static final String CUSTOMERS_1000_CSV = CUSTOMERS_1000_PREFIX + ".csv";
    private static final String CUSTOMERS_1000_DAT = CUSTOMERS_1000_PREFIX + ".dat";
    private static final String CUSTOMERS_1000_XML = CUSTOMERS_1000_PREFIX + ".xml";
    private static final String CUSTOMERS_TABLE_NAME = "CUSTOMERS";
    private static final String CUSTOMERS_1000_RESOURCE = TEST_DATA_RESOURCE_ROOT + CUSTOMERS_1000_CSV;
    private static final String CUSTOMERS_1000_SMALLINT_CONFIG = CUSTOMERS_1000_PREFIX + "-index-smallint.xml";
    private static final String CUSTOMERS_1000_SMALLINT_CONFIG_RESOURCE =
            TEST_DATA_RESOURCE_ROOT + CUSTOMERS_1000_SMALLINT_CONFIG;
    private static final String CUSTOMERS_1000_INTEGER_CONFIG = CUSTOMERS_1000_PREFIX + "-index-integer.xml";
    private static final String CUSTOMERS_1000_INTEGER_CONFIG_RESOURCE =
            TEST_DATA_RESOURCE_ROOT + CUSTOMERS_1000_INTEGER_CONFIG;

    private static FBManager fbManager;
    private static final Path databasePath = IntegrationTestProperties.databasePath("integration-test.fdb");
    private final ConfigMapper configMapper = new ConfigMapper();

    @TempDir
    static Path forAllTempDir;
    @TempDir
    Path forEachTempDir;
    private static Path customers1000CsvFile;
    private static Path customers1000SmallintConfig;
    private static Path customers1000IntegerConfig;
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
        customers1000CsvFile = forAllTempDir.resolve(CUSTOMERS_1000_CSV);
        copyResourceToPath(CUSTOMERS_1000_RESOURCE, customers1000CsvFile);
        customers1000SmallintConfig = forAllTempDir.resolve(CUSTOMERS_1000_SMALLINT_CONFIG);
        copyResourceToPath(CUSTOMERS_1000_SMALLINT_CONFIG_RESOURCE, customers1000SmallintConfig);
        customers1000IntegerConfig = forAllTempDir.resolve(CUSTOMERS_1000_INTEGER_CONFIG);
        copyResourceToPath(CUSTOMERS_1000_INTEGER_CONFIG_RESOURCE, customers1000IntegerConfig);
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
        Path tableFile = registerForCleanup(IntegrationTestProperties.externalTableFile(CUSTOMERS_1000_DAT));
        Path configFile = forEachTempDir.resolve(CUSTOMERS_1000_XML);
        createExternalTableFile(CUSTOMERS_TABLE_NAME, customers1000CsvFile, tableFile, endColumnType, configFile);
        String ddl = getDdl(configFile).replaceFirst("(?i)^\\s*create table", "recreate table");

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(CUSTOMERS_TABLE_NAME, true))) {
                assertResultSet(customers1000CsvFile, 1000, rs, endColumnType);
            }
        }
    }

    @Test
    void smallintIntegrationTest() throws Exception {
        Path tableFile = registerForCleanup(IntegrationTestProperties.externalTableFile(CUSTOMERS_1000_DAT));
        Path configOutFile = forEachTempDir.resolve(CUSTOMERS_1000_XML);
        createExternalTableFileFromExistingConfig(customers1000SmallintConfig, CUSTOMERS_TABLE_NAME,
                customers1000CsvFile, tableFile, configOutFile);
        String ddl = getDdl(configOutFile).replaceFirst("(?i)^\\s*create table", "recreate table");

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(CUSTOMERS_TABLE_NAME, true))) {
                var rsmd = rs.getMetaData();
                assertEquals(Types.SMALLINT, rsmd.getColumnType(1));
                assertResultSet(customers1000CsvFile, 1000, rs, EndColumn.Type.NONE);
            }
        }
    }

    @Test
    void integerIntegrationTest() throws Exception {
        Path tableFile = registerForCleanup(IntegrationTestProperties.externalTableFile(CUSTOMERS_1000_DAT));
        Path configOutFile = forEachTempDir.resolve(CUSTOMERS_1000_XML);
        createExternalTableFileFromExistingConfig(customers1000IntegerConfig, CUSTOMERS_TABLE_NAME,
                customers1000CsvFile, tableFile, configOutFile);
        String ddl = getDdl(configOutFile).replaceFirst("(?i)^\\s*create table", "recreate table");

        try (Connection connection = IntegrationTestProperties.createConnection(databasePath);
             var statement = connection.createStatement()) {
            statement.execute(ddl);

            try (var rs = statement.executeQuery(
                    "select * from " + statement.enquoteIdentifier(CUSTOMERS_TABLE_NAME, true))) {
                var rsmd = rs.getMetaData();
                assertEquals(Types.INTEGER, rsmd.getColumnType(1));
                assertResultSet(customers1000CsvFile, 1000, rs, EndColumn.Type.NONE);
            }
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
            return requireNonNull(informational.getDdl(), "informational/ddl");
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

    private static void copyResourceToPath(String resourceName, Path destinationPath) throws IOException {
        assert destinationPath.toAbsolutePath().startsWith(forAllTempDir)
                : "destinationPath should be rooted in forAllTempDir";
        try (InputStream in = ExtTableIntegrationTests.class.getResourceAsStream(resourceName)) {
            if (in == null) throw new FileNotFoundException("Could not find resource " + resourceName);
            Files.copy(in, destinationPath);
        }
    }

}
