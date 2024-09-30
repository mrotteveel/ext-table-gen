// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import jakarta.xml.bind.JAXBException;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseBigint;
import nl.lawinegevaar.exttablegen.convert.ParseInt128;
import nl.lawinegevaar.exttablegen.convert.ParseInteger;
import nl.lawinegevaar.exttablegen.convert.ParseSmallint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.*;
import static nl.lawinegevaar.exttablegen.EtgConfigFixtures.COLUMN_1;
import static nl.lawinegevaar.exttablegen.EtgConfigFixtures.COLUMN_2;
import static nl.lawinegevaar.exttablegen.EtgConfigFixtures.testEtgConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.emptyCavFileConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.emptyTableFile;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableColumns;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableDerivationConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableName;
import static nl.lawinegevaar.exttablegen.ResourceHelper.getResourceString;
import static nl.lawinegevaar.exttablegen.ResourceHelper.requireResourceStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigMapperTest {

    private final ConfigMapper configMapper = new ConfigMapper();

    @Test
    void testReadWriteRoundtrip() throws Exception {
        // The content of happypath-config.xml was (originally) derived from ExtTableGenTest.fromFileToFile_happyPath
        try (InputStream in = requireResourceStream("/testdata/happypath-config.xml")) {
            EtgConfig etgConfig = configMapper.read(in);
            var baos = new ByteArrayOutputStream();
            configMapper.write(etgConfig, baos);

            assertEquals(getResourceString("/testdata/happypath-config.xml"), baos.toString(UTF_8));
        }
    }

    // The following tests all work on the assumption that round tripping from EtgConfig to XML to EtgConfig should
    // produce the same output (except for the TableDerivationMode, which will always be NEVER)

    @Test
    void testHappyRoundtrip() throws Exception {
        EtgConfig originalConfig = testEtgConfig();

        EtgConfig fromXml = roundTripConfig(originalConfig);

        assertEquals(originalConfig, fromXml);
    }

    @Test
    void incompleteConfig() throws Exception {
        var originalTableConfig = TableConfig.empty();
        var originalConfig = new EtgConfig(
                originalTableConfig,
                TableDerivationConfig.getDefault(),
                Optional.empty());

        EtgConfig fromXml = roundTripConfig(originalConfig);

        assertThat(fromXml, allOf(
                tableConfig(is(originalTableConfig)),
                tableDerivationConfig(is(TableDerivationConfig.getDefault().withMode(TableDerivationMode.NEVER))),
                emptyCavFileConfig()));
    }

    /**
     * Rationale: there was a bug which removed the last column if it wasn't an end column.
     */
    @Test
    void columnListWithoutEndColumn() throws Exception {
        EtgConfig originalConfig = testEtgConfig()
                .withTableConfig(cfg -> cfg.withColumns(List.of(COLUMN_1, COLUMN_2)));

        EtgConfig fromXml = roundTripConfig(originalConfig);

        assertEquals(originalConfig, fromXml);
    }

    @ParameterizedTest
    @EnumSource(ByteOrderType.class)
    void explicitByteOrders(ByteOrderType byteOrder) throws Exception {
        EtgConfig originalConfig = testEtgConfig()
                .withTableConfig(cfg -> cfg.withByteOrder(byteOrder));

        EtgConfig fromXml = roundTripConfig(originalConfig);

        assertEquals(originalConfig, fromXml);
        assertEquals(byteOrder, fromXml.tableConfig().byteOrder());
    }

    @ParameterizedTest
    @MethodSource
    void testRoundTripWithColumn(Column column) throws Exception {
        EtgConfig originalConfig = testEtgConfig()
                .withTableConfig(cfg -> cfg.withColumns(List.of(COLUMN_1, column, COLUMN_2)));

        EtgConfig fromXml = roundTripConfig(originalConfig);

        assertEquals(originalConfig, fromXml);
        assertThat(fromXml, tableConfig(tableColumns(hasItem(column))));
    }

    static Stream<Column> testRoundTripWithColumn() {
        return Stream.of(
                smallint("COLUMN_IN", null),
                smallint("COLUMN_IN", ParseSmallint.ofRadix(16)),
                integer("COLUMN_IN", null),
                integer("COLUMN_IN", ParseInteger.ofRadix(16)),
                bigint("COLUMN_IN", null),
                bigint("COLUMN_IN", ParseBigint.ofRadix(16)),
                int128("COLUMN_IN", null),
                int128("COLUMN_IN", ParseInt128.ofRadix(16)),
                date("COLUMN_IN", null),
                date("COLUMN_IN", Converter.parseDatetime("dd-MM-yyyy", "nl-NL")),
                time("COLUMN_IN", null),
                time("COLUMN_IN", Converter.parseDatetime("h:mm:ss a", "en-US")),
                timestamp("COLUMN_IN", null),
                timestamp("COLUMN_IN", Converter.parseDatetime("MM-dd-yyyy h:mm:ss a", "en-US")),
                numeric("COLUMN_IN", 4, 1),
                numeric("COLUMN_IN", 9, 1, RoundingMode.CEILING, null),
                numeric("COLUMN_IN", 18, 1, null, Converter.parseBigDecimal("nl-NL")),
                decimal("COLUMN_IN", 9, 1),
                decimal("COLUMN_IN", 18, 1, RoundingMode.CEILING, null),
                decimal("COLUMN_IN", 38, 1, null, Converter.parseBigDecimal("nl-NL")),
                doublePrecision("COLUMN_IN", null),
                doublePrecision("COLUMN_IN", Converter.parseFloatingPointNumber("doublePrecision", "nl-NL")),
                floatCol("COLUMN_IN", null),
                floatCol("COLUMN_IN", Converter.parseFloatingPointNumber("float", "nl-NL")));
    }

    @Test
    void columnWithInvalidEncoding_throwsInvalidConfigurationException() {
        String configString =
                """
                <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                    <externalTable name="DEFAULT_EXTERNAL_TABLE_NAME">
                        <columns>
                            <column name="VALID_COLUMN">
                                <char length="2" encoding="ISO8859_1"/>
                            </column>
                            <column name="INVALID_COLUMN">
                                <char length="2" encoding="DOES_NOT_EXIST"/>
                            </column>
                        </columns>
                    </externalTable>
                </extTableGenConfig>""";

        assertThrows(InvalidConfigurationException.class, () ->
                configMapper.read(new ByteArrayInputStream(configString.getBytes(UTF_8))));
    }

    /**
     * Tests XML that should resolve to an empty/default configuration.
     * <p>
     * NOTE: We're also testing combinations which are technically invalid in the XSD (e.g. absence of
     * {@code externalTable}) to see if we're robust against incomplete or invalid configuration.
     * </p>
     */
    @ParameterizedTest
    @ValueSource(strings = {
            // Empty XML (only /extTableGenConfig)
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0"/>""",
            // Empty /extTableGenConfig/externalTable
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <externalTable/>
            </extTableGenConfig>""",
            // Empty /extTableGenConfig/externalTable/columns
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <externalTable>
                    <columns/>
                </externalTable>
            </extTableGenConfig>"""
    })
    void testIncompleteXmlResultingInEmptyConfig(String configString) throws Exception {
        EtgConfig fromXml = configMapper.read(new ByteArrayInputStream(configString.getBytes(UTF_8)));

        assertThat(fromXml, allOf(
                tableConfig(allOf(
                        tableName(is(nullValue(String.class))),
                        tableColumns(emptyCollectionOf(Column.class)),
                        emptyTableFile())),
                tableDerivationConfig(is(TableDerivationConfig.getDefault().withMode(TableDerivationMode.NEVER))),
                emptyCavFileConfig()));
    }

    /**
     * Tests XML that should resolve to an {@link InvalidConfigurationException}.
     * <p>
     * NOTE: We're also testing combinations which are technically invalid in the XSD (e.g. absence of
     * {@code externalTable}) to see if we're robust against incomplete or invalid configuration.
     * </p>
     */
    @ParameterizedTest
    @ValueSource(strings = {
            // Invalid end column type in columns
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <externalTable>
                    <columns>
                        <column name="VALID_COLUMN">
                            <char length="2" encoding="ISO8859_1"/>
                        </column>
                        <endColumn type="DOES_NOT_EXIST"/>
                    </columns>
                </externalTable>
            </extTableGenConfig>""",
            // Invalid end column in derivation config
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <tableDerivation endColumnType="DOES_NOT_EXIST"/>
            </extTableGenConfig>""",
            // Invalid encoding name in derivation config
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <tableDerivation columnEncoding="DOES_NOT_EXIST"/>
            </extTableGenConfig>""",
            // Invalid character set name
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <csvFile path="input.csv" charset="DOES_NOT_EXIST"/>
            </extTableGenConfig>""",
            // path is required if csvFile element exists
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <csvFile charset="UTF-8"/>
            </extTableGenConfig>""",
            // Unsupported schema version (using a high value to avoid having to revise this)
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="99.0"/>""",
            // Unsupported converter in char
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <externalTable>
                    <columns>
                        <column name="VALID_COLUMN">
                            <char length="2" encoding="ISO8859_1">
                                <converter>
                                    <parseIntegralNumber radix="16"/>
                                </converter>
                            </char>
                        </column>
                    </columns>
                </externalTable>
            </extTableGenConfig>""",
            // Invalid pattern in parseDatetime
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <externalTable>
                    <columns>
                        <column name="VALID_COLUMN">
                            <date>
                                <converter>
                                    <parseDatetime pattern="NOT_A_VALID_PATTERN"/>
                                </converter>
                            </date>
                        </column>
                    </columns>
                </externalTable>
            </extTableGenConfig>""",
            // Invalid mnemonic in parser
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <csvFile path="somepath" charset="UTF-8">
                    <rfc4180CsvParser quoteChar="does not exist"/>
                </csvFile>
            </extTableGenConfig>""",
            // Invalid Unicode escape in parser
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd" schemaVersion="2.0">
                <csvFile path="somepath" charset="UTF-8">
                    <customCsvParser escapeChar="u+5c"/>
                </csvFile>
            </extTableGenConfig>""",
    })
    void testInvalidXml_throwsInvalidConfigurationException(String configString) {
        assertThrows(InvalidConfigurationException.class, () ->
                configMapper.read(new ByteArrayInputStream(configString.getBytes(UTF_8))));
    }

    @Test
    void testRoundTripWithRfc4180CsvParser() throws Exception {
        EtgConfig originalConfig = testEtgConfig()
                .withCsvFileConfig(
                        cfg -> cfg.withParserConfig(CsvParserConfig.rfc4180(CharValue.of('\''), CharValue.of("TAB"))),
                        () -> { throw new IllegalStateException("Expected existing config"); });

        EtgConfig fromXml = roundTripConfig(originalConfig);

        assertEquals(originalConfig, fromXml);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, nullValues = "null", textBlock =
            """
            ignoreLeadingWhiteSpace, ignoreQuotations, strictQuotes
            true,                    false,            false
            false,                   true,             false
            false,                   false,            true
            null,                    true,             false
            false,                   null,             true
            true,                    false,            null
            """)
    void testRoundTripWithCustomCsvParser(Boolean ignoreLeadingWhiteSpace, Boolean ignoreQuotations, Boolean strictQuotes)
            throws Exception {
        EtgConfig originalConfig = testEtgConfig()
                .withCsvFileConfig(
                        cfg -> cfg.withParserConfig(CsvParserConfig.custom(CharValue.of('\''), CharValue.of("TAB"),
                                CharValue.of("U+005C"), ignoreLeadingWhiteSpace, ignoreQuotations, strictQuotes)),
                        () -> { throw new IllegalStateException("Expected existing config"); });

        EtgConfig fromXml = roundTripConfig(originalConfig);

        assertEquals(originalConfig, fromXml);
    }

    private EtgConfig roundTripConfig(EtgConfig originalConfig) throws JAXBException {
        // 3KiB rounded up from testdata/happypath-config.xml size
        var baos = new ByteArrayOutputStream(3 * 1024);
        configMapper.write(originalConfig, baos);
        return configMapper.read(new ByteArrayInputStream(baos.toByteArray()));
    }

}