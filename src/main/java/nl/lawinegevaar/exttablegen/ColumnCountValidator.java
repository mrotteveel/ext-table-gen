// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;

import java.util.function.IntPredicate;

/**
 * Validates the number of columns in a row.
 * <p>
 * This validator has two modes:
 * </p>
 * <ol>
 * <li>Expected column count known on creation of validator, {@link #requireColumns(int)} or
 * {@link #of(ExternalTable)}</li>
 * <li>Expected column count determined from first row received, {@link #fromFirstRow()}</li>
 * </ol>
 */
final class ColumnCountValidator implements RowValidator {

    private static final int UNKNOWN_COUNT = -1;
    private static final IntPredicate ALWAYS_INVALID = i -> false;

    private int columnCount;
    private IntPredicate validator;

    private ColumnCountValidator(int columnCount) {
        this.columnCount = columnCount;
        validator = columnCount == UNKNOWN_COUNT ? this::expectedSizeFromFirstRow : this::isValidRow;
    }

    /**
     * Create a column count validator from an external table definition.
     *
     * @param externalTable
     *         external table
     * @return column count validator requiring the same number of columns as the external table, excluding the last
     * column if that is an {@link EndColumn}
     */
    static ColumnCountValidator of(ExternalTable externalTable) {
        return requireColumns(externalTable.normalColumnCount());
    }

    /**
     * Create a column count validator to check if all rows have {@code columnCount} columns.
     *
     * @param columnCount
     *         required number of columns
     * @return column count validator
     * @throws IllegalArgumentException
     *         when {@code columnCount} is less than 1
     */
    static ColumnCountValidator requireColumns(int columnCount) {
        if (columnCount < 1) {
            throw new IllegalArgumentException(
                    "Column count must be a positive non-zero number, received: %d".formatted(columnCount));
        }
        return new ColumnCountValidator(columnCount);
    }

    /**
     * Create a column count validator to check if all rows have the same column count as the first row.
     * <p>
     * If the first row is empty, it is rejected, as at least 1 column is required. The first row is either
     * the header row or the first data row. This is contrary to usage of the term <em>first row</em> elsewhere in
     * ext-table-gen as this is an OpenCSV validator, which always considers the first row as simply a row.
     * </p>
     *
     * @return column count validator
     */
    static ColumnCountValidator fromFirstRow() {
        return new ColumnCountValidator(UNKNOWN_COUNT);
    }

    @Override
    public boolean isValid(String[] row) {
        return validator.test(row.length);
    }

    private boolean expectedSizeFromFirstRow(int rowColumnCount) {
        if (rowColumnCount == 0) {
            // Don't allow "empty" first row, we expect at least 1 column.
            // Setting column count to 1 so validation exception message makes sense.
            columnCount = 1;
            // Ensure that if subsequent rows are tested, they all test as invalid
            validator = ALWAYS_INVALID;
            return false;
        }
        columnCount = rowColumnCount;
        validator = this::isValidRow;
        return true;
    }

    private boolean isValidRow(int rowColumnCount) {
        return rowColumnCount == columnCount;
    }

    @Override
    public void validate(String[] row) throws CsvValidationException {
        if (isValid(row)) return;
        throw new InvalidCsvColumnCountException(
                "Invalid column count, expected: %d, received %d".formatted(columnCount, row.length));
    }

}
