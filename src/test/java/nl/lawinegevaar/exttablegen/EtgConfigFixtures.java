// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.col;

final class EtgConfigFixtures {

    static final String TABLE_NAME = "FIXTURE_TABLE_NAME";
    static final Column COLUMN_1 = col("FIXTURE_COLUMN_1", 10);
    static final Column COLUMN_2 = col("FIXTURE_COLUMN_2", 15, FbEncoding.UTF8);
    static final Column END_COLUMN = EndColumn.require(EndColumn.Type.CRLF);
    static final Path TABLE_FILE_PATH = Path.of("fixture-output.dat");
    static final Path INPUT_PATH = Path.of("fixture-input.csv");
    static final boolean OVERWRITE_TABLE_FILE = true;
    static final boolean INPUT_HAS_HEADER = false;
    static final EndColumn.Type DERIVATION_COLUMN_TYPE = EndColumn.Type.CRLF;
    static final FbEncoding DERIVATION_ENCODING = FbEncoding.ASCII;
    static final TableDerivationMode DERIVATION_MODE = TableDerivationMode.NEVER;

    private EtgConfigFixtures() {
        // no instances
    }

    static EtgConfig testEtgConfig() {
        return new EtgConfig(testTableConfig(), testDerivationConfig(), testInputConfig());
    }

    static TableConfig testTableConfig() {
        return new TableConfig(
                TABLE_NAME,
                testColumns(),
                testTableFile());
    }

    static List<Column> testColumns() {
        return List.of(COLUMN_1, COLUMN_2, END_COLUMN);
    }

    static TableFile testTableFile() {
        return new TableFile(TABLE_FILE_PATH, OVERWRITE_TABLE_FILE);
    }

    static TableDerivationConfig testDerivationConfig() {
        return new TableDerivationConfig(DERIVATION_ENCODING, DERIVATION_COLUMN_TYPE, DERIVATION_MODE);
    }

    static InputConfig testInputConfig() {
        return new InputConfig(INPUT_PATH, US_ASCII, INPUT_HAS_HEADER);
    }

}
