// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;
import nl.lawinegevaar.exttablegen.type.FbChar;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Validates the column sizes of a row.
 */
final class ColumnSizeValidator implements RowValidator {

    private final int[] sizes;

    /**
     * Expected maximum column sizes.
     *
     * @param sizes
     *         array of maximum sizes in Unicode codepoints, a value of {@code -1} signifies no validation; if
     *         an offered row has more or less columns, those are ignored, use {@link ColumnCountValidator} to validate
     *         column count
     */
    ColumnSizeValidator(int[] sizes) {
        this(sizes, true);
    }

    private ColumnSizeValidator(int[] sizes, boolean clone) {
        this.sizes = clone ? sizes.clone() : sizes;
    }

    /**
     * Creates a column size validator from the columns of {@code externalTable}.
     *
     * @param externalTable
     *         external table definition
     * @return a column size validator
     */
    static ColumnSizeValidator of(ExternalTable externalTable) {
        int[] sizes = externalTable.columns().stream()
                .limit(externalTable.normalColumnCount())
                .mapToInt(ColumnSizeValidator::expectedSize)
                .toArray();
        return new ColumnSizeValidator(sizes, false);
    }

    private static int expectedSize(Column column) {
        if (column.datatype() instanceof FbChar fbChar) {
            return fbChar.length();
        }
        // TODO Check max length for integer types (i.e. sign + maximum number of digits)?
        //  Doing so might limit or complicate future changes with parsing
        return -1;
    }

    int[] sizes() {
        return sizes.clone();
    }

    @Override
    public boolean isValid(String[] row) {
        return validate0(row).isEmpty();
    }

    @Override
    public void validate(String[] row) throws CsvValidationException {
        List<ColumnResult> invalidColumnResults = validate0(row);
        if (!invalidColumnResults.isEmpty()) {
            throw new InvalidCsvColumnSizeException(
                    "One or more columns exceeded the maximum size: " + invalidColumnResults);
        }
    }

    private List<ColumnResult> validate0(String[] row) {
        return IntStream.range(0, Math.min(row.length, sizes.length))
                .filter(i -> exceedsSize(sizes[i], row[i]))
                .mapToObj(i -> new ColumnResult(i, sizes[i], row[i].length()))
                .toList();
    }

    private boolean exceedsSize(int maxSize, String value) {
        if (maxSize == -1 || value == null) {
            return false;
        }
        int length = value.length();
        // We check length in char first, to avoid the overhead of determining code points if it isn't needed (because
        // length in char will always be greater or equal to code point count)
        return length > maxSize && value.codePointCount(0, length) > maxSize;
    }

    private record ColumnResult(int index, int maximumSize, int actualSize) {
    }

}
