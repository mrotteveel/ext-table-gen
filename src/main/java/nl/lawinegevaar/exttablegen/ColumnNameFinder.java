// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;

/**
 * Finds the column names from the header row, or &mdash; if there is no header row &mdash; generates column names.
 * <p>
 * Generated column names are of the form <code>COLUMN_<i>n</i></code>, where <i>n</i> is the 1-based position of
 * the column.
 * </p>
 * <p>
 * The implementation will signal {@code Unsubscribe} to its publisher after the column names have been found.
 * </p>
 */
final class ColumnNameFinder extends AbstractRowProcessor {

    private List<String> columnNames = emptyList();

    @Override
    public ProcessingResult onHeader(Row header) {
        if (!columnNames().isEmpty()) {
            throw new IllegalStateException("onHeader was invoked after column names were found");
        }
        // empty or no header, generate from column count of first row instead
        if (header.isEmpty()) return ProcessingResult.continueProcessing();
        
        if (header.stream().anyMatch(String::isBlank)) {
            return ProcessingResult.stopWith(
                    new NoColumnNamesException(
                            "Could not get column names: one or more header columns were empty or blank. Received: "
                            + header));
        }
        columnNames = header.stream().map(String::trim).toList();
        return ProcessingResult.unsubscribe();
    }

    @Override
    public ProcessingResult onRow(Row row) {
        if (!columnNames.isEmpty()) {
            // Already have column names, caller probably ignored unsubscribe, submit again
            return ProcessingResult.unsubscribe();
        }
        if (row.isEmpty()) {
            return ProcessingResult.stopWith(
                    new NoColumnNamesException("Could not get or derive column names: received an empty row"));
        }
        columnNames = IntStream.rangeClosed(1, row.size())
                .mapToObj(idx -> "COLUMN_" + idx)
                .toList();
        // We're no longer interested in rows
        return ProcessingResult.unsubscribe();
    }

    /**
     * Column names found or generated.
     *
     * @return list of found or generated columns, empty if no rows were received
     * @throws NoColumnNamesException
     *         if no column names have been found, but an exception was received
     */
    List<String> columnNames() {
        // when columnNames is not empty, the Unsubscribe signal was probably ignored, and we may have received
        // subsequent exceptions
        if (columnNames.isEmpty()) {
            getLastException().ifPresent(e -> {
                throw new NoColumnNamesException(
                        "No column names found, but an exception was received", e);
            });
        }
        return columnNames;
    }

}
