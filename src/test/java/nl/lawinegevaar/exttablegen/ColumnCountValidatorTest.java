// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnCountValidatorTest {

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -2 })
    void requireColumns_rejectIllegalValues(int invalidColumnCount) {
        assertThrows(IllegalArgumentException.class,
                () -> ColumnCountValidator.requireColumns(invalidColumnCount));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10 })
    void requireColumns_acceptsRequiredColumnCount(int columnCount) {
        var validator = ColumnCountValidator.requireColumns(columnCount);
        var row = new String[columnCount];

        assertTrue(validator.isValid(row), "expected row of required column count to be valid");
        assertDoesNotThrow(() -> validator.validate(row));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 1 })
    void requireColumns_rejectsInvalidColumnCount(int countOffset) {
        var validator = ColumnCountValidator.requireColumns(5);
        var row = new String[5 + countOffset];

        assertFalse(validator.isValid(row), "expected row of wrong column count to be invalid");
        assertThrows(CsvValidationException.class, () -> validator.validate(row));
    }

    @Test
    void fromFirstRow_rejectFirstRowEmpty() {
        var emptyRow = new String[0];

        // NOTE: Testing isValid separately from validate, as it is a stateful operation for the first row
        assertFalse(ColumnCountValidator.fromFirstRow().isValid(emptyRow), "expected empty first row to be invalid");
        assertThrows(CsvValidationException.class, () -> ColumnCountValidator.fromFirstRow().validate(emptyRow));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10 })
    void fromFirstRow_acceptFirstRowNotEmpty(int columnCount) {
        var row = new String[columnCount];

        // NOTE: Testing isValid separately from validate, as it is a stateful operation for the first row
        assertTrue(ColumnCountValidator.fromFirstRow().isValid(row), "expected non-empty first row to be valid");
        assertDoesNotThrow(() -> ColumnCountValidator.fromFirstRow().validate(row));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10 })
    void fromFirstRow_acceptsColumnCountMatchingFirstRow(int columnCount) {
        var validator = ColumnCountValidator.fromFirstRow();
        var row = new String[columnCount];
        // prime validator
        assertDoesNotThrow(() -> validator.validate(row), "should not reject first row");

        assertTrue(validator.isValid(row), "expected row of required column count to be valid");
        assertDoesNotThrow(() -> validator.validate(row));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 1 })
    void fromFirstRow_rejectsInvalidColumnCount(int countOffset) {
        var validator = ColumnCountValidator.fromFirstRow();
        assertDoesNotThrow(() -> validator.validate(new String[5]), "should not reject first row");
        var secondRow = new String[5 + countOffset];

        assertFalse(validator.isValid(secondRow), "expected row of wrong column count to be invalid");
        assertThrows(CsvValidationException.class, () -> validator.validate(secondRow));
    }
}