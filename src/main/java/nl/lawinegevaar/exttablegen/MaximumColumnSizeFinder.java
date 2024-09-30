// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Finds (determines) the maximum column sizes in Unicode codepoints from the actual received data.
 * <p>
 * Instances of this class should be used only once; create a new instance when processing again (no matter if it is
 * the same or a different file).
 * </p>
 */
final class MaximumColumnSizeFinder extends AbstractRowProcessor {

    private static final int[] NOT_SET = new int[0];
    private int[] columnSizes = NOT_SET;

    /**
     * Creates a maximum column size consumer with initial size 0.
     */
    MaximumColumnSizeFinder() {
    }

    /**
     * Returns a copy of the current maximum column sizes in Unicode codepoints.
     *
     * @return a copy of the current maximum column sizes.
     */
    int[] getMaximumColumnSizes() {
        return columnSizes.clone();
    }

    @Override
    public ProcessingResult onHeader(Row header) {
        columnSizes = new int[header.size()];
        return ProcessingResult.continueProcessing();
    }

    @Override
    public ProcessingResult onRow(Row row) {
        if (columnSizes.length < row.size()) {
            columnSizes = Arrays.copyOf(columnSizes, row.size());
        }
        for (int i = 0; i < row.size(); i++) {
            updateColumnSize(i, row.get(i));
        }
        return ProcessingResult.continueProcessing();
    }

    private void updateColumnSize(int index, @Nullable String columnValue) {
        if (columnValue == null) return;
        int lengthInChar = columnValue.length();
        // checking length in char first, because length in char is always greater than or equal to Unicode code points
        if (columnSizes[index] >= lengthInChar) return;

        int lengthInCodePoints = columnValue.codePointCount(0, lengthInChar);
        if (columnSizes[index] < lengthInCodePoints) {
            columnSizes[index] = lengthInCodePoints;
        }
    }
    
}
