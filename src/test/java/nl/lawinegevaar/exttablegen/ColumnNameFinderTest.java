// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColumnNameFinderTest {

    private final ColumnNameFinder nameFinder = new ColumnNameFinder();

    @Test
    void initialColumnsAreEmpty() {
        assertEquals(emptyList(), nameFinder.columnNames(), "expected empty list of column names");
    }

    @Test
    void onHeaderWithNoHeaderValue_columnsAreEmpty() {
        assertInstanceOf(ProcessingResult.Continue.class, nameFinder.onHeader(Row.noHeader()));

        assertEquals(emptyList(), nameFinder.columnNames(),
                "expected empty list of column names after receiving 'not a header' header");
    }

    @Test
    void onHeaderWithEmptyHeader_columnsAreEmpty() {
        assertInstanceOf(ProcessingResult.Continue.class, nameFinder.onHeader(new Row(1, List.of())));

        assertEquals(emptyList(), nameFinder.columnNames(),
                "expected empty list of column names after receiving empty header");
    }

    @Test
    void namesFromHeaderRow() {
        var columnNames = List.of("first column", "second column", "third column");

        assertInstanceOf(ProcessingResult.Unsubscribe.class, nameFinder.onHeader(new Row(1, columnNames)));

        assertEquals(columnNames, nameFinder.columnNames(), "unexpected column names");
    }

    @Test
    void namesFromHeaderRow_ignoresSubsequentRows() {
        var columnNames = List.of("first column", "second column", "third column");
        assertInstanceOf(ProcessingResult.Unsubscribe.class, nameFinder.onHeader(new Row(1, columnNames)));

        assertInstanceOf(ProcessingResult.Unsubscribe.class,
                nameFinder.onRow(new Row(2, List.of("first value", "second value", "third value"))));

        assertEquals(columnNames, nameFinder.columnNames(), "unexpected column names");
    }

    @Test
    void namesFromHeaderRow_ignoresExceptionAfterReceivingHeaderRow() {
        var columnNames = List.of("first column", "second column", "third column");
        assertInstanceOf(ProcessingResult.Unsubscribe.class, nameFinder.onHeader(new Row(1, columnNames)));

        assertInstanceOf(ProcessingResult.Continue.class, nameFinder.onException(new RuntimeException("test")));

        assertEquals(columnNames, nameFinder.columnNames(), "unexpected column names");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            '', second column, third column
            first column, '', third column
            first column, second column, ''
            first column, ' ', third column
            first column, '   ', third column
            """)
    void namesFromHeaderRow_blankValueInRow_stopWithNoColumnNamesException(String first, String second, String third) {
        var stopResult = assertInstanceOf(ProcessingResult.StopWithException.class,
                nameFinder.onHeader(new Row(1, List.of(first, second, third))));

        assertEquals(emptyList(), nameFinder.columnNames(),
                "expected empty list of column names after receiving row with empty columns");
        assertInstanceOf(NoColumnNamesException.class, stopResult.exception(), "unexpected exception type in result");

    }

    @Test
    void namesGeneratedFromFirstRowColumnCount() {
        assertInstanceOf(ProcessingResult.Continue.class, nameFinder.onHeader(Row.noHeader()));
        assertInstanceOf(ProcessingResult.Unsubscribe.class,
                nameFinder.onRow(new Row(1, List.of("first value", "second value", "third value"))));

        assertEquals(List.of("COLUMN_1", "COLUMN_2", "COLUMN_3"), nameFinder.columnNames(), "unexpected column names");
    }

    @Test
    void namesGeneratedFromFirstRowColumnCount_ignoresSubsequentRows() {
        assertInstanceOf(ProcessingResult.Continue.class, nameFinder.onHeader(Row.noHeader()));
        assertInstanceOf(ProcessingResult.Unsubscribe.class,
                nameFinder.onRow(new Row(1, List.of("first value", "second value", "third value"))));

        assertInstanceOf(ProcessingResult.Unsubscribe.class,
                nameFinder.onRow(new Row(2, List.of("first value", "second value", "third value", "fourth value"))));

        assertEquals(List.of("COLUMN_1", "COLUMN_2", "COLUMN_3"), nameFinder.columnNames(), "unexpected column names");
    }

    @Test
    void namesGeneratedFromFirstRowColumnCount_afterException() {
        assertInstanceOf(ProcessingResult.Continue.class, nameFinder.onException(new RuntimeException("test")));

        assertThrows(NoColumnNamesException.class, nameFinder::columnNames);

        ProcessingResult result = nameFinder.onRow(new Row(2, List.of("first value", "second value")));

        assertEquals(List.of("COLUMN_1", "COLUMN_2"), nameFinder.columnNames(), "unexpected column names");
        assertInstanceOf(ProcessingResult.Unsubscribe.class, result, "Expected Unsubscribe result");
    }

    @Test
    void firstRowIsEmpty_stopWithNoColumnNamesException() {
        assertInstanceOf(ProcessingResult.Continue.class, nameFinder.onHeader(Row.noHeader()));
        var stopResult = assertInstanceOf(ProcessingResult.StopWithException.class,
                nameFinder.onRow(new Row(1, List.of())));

        assertEquals(emptyList(), nameFinder.columnNames(),
                "expected empty list of column names after receiving empty row");
        assertInstanceOf(NoColumnNamesException.class, stopResult.exception(), "unexpected exception type in result");
    }

}