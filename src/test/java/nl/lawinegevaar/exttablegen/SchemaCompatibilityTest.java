// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbEncoding;
import nl.lawinegevaar.exttablegen.xmlconfig.ExtTableGenConfig;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.customers10Columns;
import static nl.lawinegevaar.exttablegen.ResourceHelper.requireResourceStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests if documents created with older versions of the {@code https://www.lawinegevaar.nl/xsd/ext-table-gen-1.0.xsd}
 * can still be loaded.
 */
class SchemaCompatibilityTest {

    private final ConfigMapper configMapper = new ConfigMapper();

    @Test
    void config_xsd1_0_v1_0() throws Exception {
        try (InputStream in = requireResourceStream("/testdata/compat/config-xsd1.0-v1.0.xml")) {
            ExtTableGenConfig extTableGenConfig = configMapper.readAsExtTableGenConfig(in);
            assertEquals(ConfigMapper.SCHEMA_VERSION_1_0, extTableGenConfig.getSchemaVersion(),
                    "Unmarshalling should set /extTableGenConfig[@schemaVersion] for version 1.0 file");

            EtgConfig etgConfig = configMapper.fromXmlExtTableGenConfig(extTableGenConfig);

            assertEquals(
                    new EtgConfig(
                            new TableConfig("DEFAULT_EXTERNAL_TABLE_NAME",
                                    customers10Columns(EndColumn.Type.LF, FbEncoding.ISO8859_1),
                                    new TableFile(Path.of("output.dat"), false), ByteOrderType.AUTO),
                            TableDerivationConfig.getDefault().withMode(TableDerivationMode.NEVER),
                            new CsvFileConfig(Path.of("input.csv"), UTF_8, true, CsvParserConfig.of())),
                    etgConfig,
                    "Unexpected configuration for version 1.0 file");
        }
    }

}
