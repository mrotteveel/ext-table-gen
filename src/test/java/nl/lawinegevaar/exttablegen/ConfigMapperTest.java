// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.lawinegevaar.exttablegen.EtgConfigFixtures.COLUMN_1;
import static nl.lawinegevaar.exttablegen.EtgConfigFixtures.COLUMN_2;
import static nl.lawinegevaar.exttablegen.EtgConfigFixtures.testEtgConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.emptyCavFileConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.emptyTableFile;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableColumns;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableDerivationConfig;
import static nl.lawinegevaar.exttablegen.EtgConfigMatchers.tableName;
import static nl.lawinegevaar.exttablegen.ExternalTable.DEFAULT_TABLE_NAME;
import static nl.lawinegevaar.exttablegen.ResourceHelper.getResourceString;
import static nl.lawinegevaar.exttablegen.ResourceHelper.requireResourceStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    // The following tests in all work on the assumption that round tripping from EtgConfig to XML to EtgConfig should
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

    @Test
    void columnWithInvalidEncoding_returnEmptyColumns() throws Exception {
        String configString =
                """
                <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
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

        EtgConfig fromXml = configMapper.read(new ByteArrayInputStream(configString.getBytes(UTF_8)));

        assertThat(fromXml, allOf(
                tableConfig(allOf(
                        tableName(is(DEFAULT_TABLE_NAME)),
                        tableColumns(emptyCollectionOf(Column.class)),
                        emptyTableFile())),
                tableDerivationConfig(is(TableDerivationConfig.getDefault().withMode(TableDerivationMode.NEVER))),
                emptyCavFileConfig()));
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
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd"/>""",
            // Empty /extTableGenConfig/externalTable
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
                <externalTable/>
            </extTableGenConfig>""",
            // Empty /extTableGenConfig/externalTable/columns
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
                <externalTable>
                    <columns/>
                </externalTable>
            </extTableGenConfig>""",
            // Invalid end column type in columns
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
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
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
                <tableDerivationConfig endColumnType="DOES_NOT_EXIST"/>
            </extTableGenConfig>""",
            // Invalid encoding name in derivation config
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
                <tableDerivationConfig columnEncoding="DOES_NOT_EXIST"/>
            </extTableGenConfig>""",
            // Invalid character set name
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
                <csvFile path="input.csv" charset="DOES_NOT_EXIST"/>
            </extTableGenConfig>""",
            // path is required if csvFile element exists
            """
            <extTableGenConfig xmlns="https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd">
                <csvFile charset="UTF-8"/>
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

    private EtgConfig roundTripConfig(EtgConfig originalConfig) throws JAXBException {
        // 3KiB rounded up from testdata/happypath-config.xml size
        var baos = new ByteArrayOutputStream(3 * 1024);
        configMapper.write(originalConfig, baos);
        return configMapper.read(new ByteArrayInputStream(baos.toByteArray()));
    }

}