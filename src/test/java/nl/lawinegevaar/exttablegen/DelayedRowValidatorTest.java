// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelayedRowValidatorTest {

    @SuppressWarnings("DataFlowIssue")
    @Test
    void delay_null_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DelayedRowValidator.delay(null));
    }

    @Test
    void delay_withDelayedRowValidator_throwsIllegalArgumentException() {
        var validator = new ColumnSizeValidator(new int[] { 5 });
        RowValidator delayedRowValidator = DelayedRowValidator.delay(validator).untilAfterRow(5);

        assertThrows(IllegalArgumentException.class, () -> DelayedRowValidator.delay(delayedRowValidator));
    }

    @Test
    void untilAfterRow_lessThanZero_throwsIllegalArgumentException() {
        var validator = new ColumnSizeValidator(new int[] { 5 });

        DelayedRowValidator.Builder builder = DelayedRowValidator.delay(validator);

        assertThrows(IllegalArgumentException.class, () -> builder.untilAfterRow(-1));
    }

    @Test
    void untilAfterRow_zero_returnsOriginalValidator() {
        var validator = new ColumnSizeValidator(new int[] { 5 });

        assertSame(validator, DelayedRowValidator.delay(validator).untilAfterRow(0));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 5 })
    void untilAfterRow_positive_delaysValidation(int skipRows) {
        var validator = new ColumnSizeValidator(new int[] { 5 });
        // String which will trigger validator (as 6 > 5)
        String invalidString = "X".repeat(6);

        RowValidator delayedRowValidator = DelayedRowValidator.delay(validator).untilAfterRow(skipRows);

        int row;
        for (row = 1; row <= skipRows; row++) {
            assertDoesNotThrow(() -> delayedRowValidator.validate(new String[] { invalidString }),
                    "row %d should not have thrown a validation exception".formatted(row));
        }
        assertThrows(CsvValidationException.class, () -> delayedRowValidator.validate(new String[] { invalidString }),
                "row %d should have thrown a validation exception".formatted(row));
    }

}