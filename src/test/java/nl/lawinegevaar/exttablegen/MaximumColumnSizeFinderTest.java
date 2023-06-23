// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MaximumColumnSizeFinderTest {

    @Test
    void initiallyNoColumns() {
        var sizeFinder = new MaximumColumnSizeFinder();

        assertColumnSizes(new int[0], sizeFinder, "expected empty array");
    }

    @Test
    void onException_continueProcessing() {
        var sizeFinder = new MaximumColumnSizeFinder();
        assertInstanceOf(ProcessingResult.Continue.class, sizeFinder.onException(new CsvValidationException("test")),
                "expected onException to signal continue processing");
    }

    @Test
    void testColumnSizeDetermination() {
        var sizeFinder = new MaximumColumnSizeFinder();

        sizeFinder.onRow(new Row(1, List.of("1", "12", "123", "1234")));
        assertColumnSizes(new int[] { 1, 2, 3, 4 }, sizeFinder, "unexpected column sizes");

        sizeFinder.onRow(new Row(2, List.of("", "1", "1234", "123", "")));
        assertColumnSizes(new int[] { 1, 2, 4, 4, 0 }, sizeFinder, "unexpected column sizes");

        sizeFinder.onRow(new Row(3, List.of()));
        assertColumnSizes(new int[] { 1, 2, 4, 4, 0 }, sizeFinder, "unexpected column sizes");

        sizeFinder.onRow(new Row(4, List.of("12", "12", "123", "12345", "1", "12345678")));
        assertColumnSizes(new int[] { 2, 2, 4, 5, 1, 8 }, sizeFinder, "unexpected column sizes");
    }

    @Test
    void testColumnSizesUseUnicodeCodepoints() {
        var sizeFinder = new MaximumColumnSizeFinder();

        // NOTE: \uD83D\uDE00 is two chars (surrogate pairs), but a single Unicode codepoint (U+1F600)
        sizeFinder.onRow(new Row(1, List.of("\uD83D\uDE00")));
        assertColumnSizes(new int[] { 1 }, sizeFinder, "expected surrogate pairs to be counted as a single codepoint");
    }

    private void assertColumnSizes(int[] expectedColumnSizes, MaximumColumnSizeFinder sizeFinder, String message) {
        assertArrayEquals(expectedColumnSizes, sizeFinder.getMaximumColumnSizes(), message);
    }

}