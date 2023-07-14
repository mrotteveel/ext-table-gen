// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbChar;
import nl.lawinegevaar.exttablegen.type.FbEncoding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColumnSizeValidatorTest {

    @Test
    void validate_happyPath() {
        var validator = new ColumnSizeValidator(new int[] { 3, 4, 5 });

        assertArrayEquals(new int[] { 3, 4, 5 }, validator.sizes());
        for (String[] row : new String[][] {
                { "", "", "" },
                { "1", "1", "1" },
                { "123", "1234", "12345" },
                // all null
                { null, null, null },
                // too many columns does not produce error
                { "1", "1", "1", "1" },
                // too few columns does not produce error
                { "1", "1" },
                // empty does not produce error (special case of previous)
                {},
        }) {
            assertDoesNotThrow(() -> validator.validate(row));
        }
    }

    @Test
    void validate_failingRow() {
        var validator = new ColumnSizeValidator(new int[] { 3, 4, 5 });

        var exception = assertThrows(InvalidCsvColumnSizeException.class,
                () -> validator.validate(new String[] { "123", "12345", "123456" }));
        assertEquals("One or more columns exceeded the maximum size: [ColumnResult[index=1, maximumSize=4, actualSize=5], ColumnResult[index=2, maximumSize=5, actualSize=6]]",
                exception.getMessage());
    }

    @Test
    void validate_ignoreSize() {
        var validator = new ColumnSizeValidator(new int[] { 1, -1 });

        assertArrayEquals(new int[] { 1, -1 }, validator.sizes());
        assertDoesNotThrow(() -> validator.validate(new String[] { "1", "12345" }));
        assertThrows(InvalidCsvColumnSizeException.class, () -> validator.validate(new String[] { "12", "12" }));
    }

    @Test
    void ofExternalTable() {
        var externalTable = new ExternalTable(
                "TEST",
                List.of(
                        new Column("COL1", new FbChar(5, FbEncoding.ASCII)),
                        new Column("COL2", new FbChar(11, FbEncoding.ASCII)),
                        EndColumn.require(EndColumn.Type.CRLF)),
                OutputResource.nullOutputResource(),
                ByteOrderType.AUTO);

        var validator = ColumnSizeValidator.of(externalTable);

        assertArrayEquals(new int[] { 5, 11 }, validator.sizes());
    }

}