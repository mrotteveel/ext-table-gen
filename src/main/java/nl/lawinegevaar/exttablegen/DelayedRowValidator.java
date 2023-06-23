// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;

/**
 * A row validator wrapping another row validator to delay validation until a number of rows have been received.
 * <p>
 * The delay only applies to {@link RowValidator#validate(String[])}.
 * </p>
 */
final class DelayedRowValidator implements RowValidator {

    private final RowValidator delegate;
    private int skipRows;

    private DelayedRowValidator(RowValidator delegate, int skipRows) {
        if (delegate instanceof DelayedRowValidator) {
            throw new IllegalArgumentException("DelayedRowValidator does not accept a DelayedRowValidator instance");
        }
        if (skipRows <= 0) {
            throw new IllegalArgumentException("skipRows must be greater than 0, was " + skipRows);
        }
        this.delegate = delegate;
        this.skipRows = skipRows;
    }

    static Builder delay(RowValidator delegate) {
        return new Builder(delegate);
    }

    @Override
    public boolean isValid(String[] row) {
        return delegate.isValid(row);
    }

    @Override
    public void validate(String[] row) throws CsvValidationException {
        if (skipRows == 0) {
            delegate.validate(row);
        } else {
            skipRows--;
        }
    }

    record Builder(RowValidator delegate) {

        /**
         * Delays invoking the row validator until after a specified number of rows have been seen.
         *
         * @param row
         *         number of rows to ignore before using {@code delegate}
         * @return the delayed row validator, or the delegate if {@code rows == 0}
         */
        RowValidator untilAfterRow(int row) {
            if (row == 0) {
                return delegate;
            }
            return new DelayedRowValidator(delegate, row);
        }

    }
}
