// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.Logger.Level.TRACE;

/**
 * Derives an external table definition from row information.
 */
final class ExternalTableProcessor extends MultiplexRowProcessor {

    private final ExternalTable.Config tableConfig;
    private final ColumnNameFinder columnNameFinder = new ColumnNameFinder();
    private final MaximumColumnSizeFinder columnSizeFinder = new MaximumColumnSizeFinder();

    ExternalTableProcessor(ExternalTable.Config tableConfig) {
        this.tableConfig = tableConfig;
        subscribe(columnNameFinder);
        subscribe(columnSizeFinder);
    }

    /**
     * Gets the external table.
     * <p>
     * Should only be called after this processor has been populated.
     * </p>
     *
     * @return external table definition
     * @throws IllegalArgumentException
     *         when called before completion of this row processor
     * @throws NoColumnNamesException
     *         when no column names were found (likely due to an empty file)
     */
    ExternalTable getExternalTable() {
        if (completionCount() == 0) {
            throw new IllegalStateException("getExternalTable() called before completion of ExternalTableProcessor");
        }
        List<String> columnNames = columnNameFinder.columnNames();
        int[] columnSizes = columnSizeFinder.getMaximumColumnSizes();
        if (columnNames.isEmpty()) {
            throw new NoColumnNamesException("Could not find column names, source file is probably empty");
        }

        if (columnNames.size() != columnSizes.length) {
            System.getLogger(ExternalTable.class.getName())
                    .log(TRACE,
                            ("Mismatch between column name count and column sizes count: %d <> %d, overflow columns "
                             + "will be ignored, underflow columns will be empty")
                                    .formatted(columnNames.size(), columnSizes.length));
        }

        var columns = new ArrayList<Column>();
        for (int i = 0; i < columnNames.size(); i++) {
            String columName = columnNames.get(i);
            int columnSize = i < columnSizes.length ? columnSizes[i] : -1;

            columns.add(createColumn(columName, columnSize));
        }
        tableConfig.endColumn().ifPresent(columns::add);

        return createExternalTable(columns);
    }

    private Column createColumn(String columnName, int columnSize) {
        return new Column(columnName, new FbChar(Math.max(1, columnSize), tableConfig.defaultEncoding()));
    }

    private ExternalTable createExternalTable(List<Column> columns) {
        return new ExternalTable(tableConfig.tableName(), columns, tableConfig.outputResource(),
                tableConfig.byteOrder());
    }

}
