// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;

import static java.util.Objects.requireNonNull;

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
        // assume that validation happens in the builder
        this.delegate = delegate;
        this.skipRows = skipRows;
    }

    /**
     * Creates a builder for a delayed row validator.
     *
     * @param delegate
     *         validator to delegate to
     * @return a builder for a delayed row validator
     * @throws NullPointerException
     *         if {@code delegate} is {@code null}
     * @throws IllegalArgumentException
     *         if {@code delegate} is a {@code DelayedRowValidator}
     */
    static Builder delay(RowValidator delegate) {
        return new Builder(delegate);
    }

    @Override
    public boolean isValid(String[] row) {
        return delegate.isValid(row);
    }

    @Override
    public void validate(String[] row) throws CsvValidationException {
        if (skipRows > 0) {
            skipRows--;
        } else {
            delegate.validate(row);
        }
    }

    record Builder(RowValidator delegate) {

        Builder {
            if (requireNonNull(delegate, "delegate") instanceof DelayedRowValidator) {
                throw new IllegalArgumentException(
                        "DelayedRowValidator.Builder does not accept a DelayedRowValidator instance");
            }
        }

        /**
         * Delays invoking the row validator until after a specified number of rows have been seen.
         *
         * @param row
         *         number of rows to ignore before using {@code delegate}
         * @return the delayed row validator, or the delegate if {@code rows == 0}
         * @throws IllegalArgumentException
         *         if {@code row < 0}
         */
        RowValidator untilAfterRow(int row) {
            if (row < 0) throw new IllegalArgumentException("row must be equal to or greater than 0, was " + row);
            if (row == 0) return delegate;

            return new DelayedRowValidator(delegate, row);
        }

    }
}
