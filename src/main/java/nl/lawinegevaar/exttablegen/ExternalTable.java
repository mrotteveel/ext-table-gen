// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Represents an external table.
 */
record ExternalTable(String name, List<Column> columns, OutputResource outputResource, ByteOrderType byteOrder) {

    static final String DEFAULT_TABLE_NAME = "DEFAULT_EXTERNAL_TABLE_NAME";

    /**
     * Creates an external table definition.
     *
     * @param name
     *         table name, if {@code null} or blank, the name {@code "DEFAULT_EXTERNAL_TABLE_NAME"} is used, the name is
     *         trimmed
     * @param columns
     *         columns of the table (cannot be {@code null})
     * @param outputResource
     *         output resource of the external table (can be {@code null})
     * @throws IllegalArgumentException
     *         When {@code columns} is empty, or contains an {@link EndColumn} <em>before</em> the last column
     */
    ExternalTable {
        if (outputResource == null) {
            outputResource = OutputResource.nullOutputResource();
        }

        if (requireNonNull(columns, "columns").isEmpty()) {
            throw new IllegalArgumentException("External table requires at least one column");
        }
        if (columns.subList(0, columns.size() - 1).stream().anyMatch(column -> column instanceof EndColumn)) {
            throw new IllegalArgumentException("An EndColumn may only occur as the last column in columns");
        }
        columns = List.copyOf(columns);

        name = name != null ? name.trim() : null;
        if (name == null || name.isBlank()) {
            name = DEFAULT_TABLE_NAME;
        }
    }

    /**
     * Creates a copy of this external table definition, using {@code outputResource}.
     *
     * @param outputResource
     *         output resource of the external table
     * @return new external table derived from this instance, with {@code outputResource} replaced, or this instance if
     * {@code outputResource} is the same as in this instance
     */
    ExternalTable withOutputResource(OutputResource outputResource) {
        if (this.outputResource == outputResource) {
            return this;
        }
        return new ExternalTable(name, columns, outputResource, byteOrder);
    }

    /**
     * @return number of columns in the external table
     */
    int columnCount() {
        return columns.size();
    }

    /**
     * @return number of normal columns in the external table (that is, excluding the {@link EndColumn} if it has one)
     */
    int normalColumnCount() {
        int size = columns.size();
        return size > 0 && columns.get(size - 1) instanceof EndColumn ? size - 1 : size;
    }

    /**
     * @return quoted column name (if {@link #name()} is already quoted, it is returned as-is)
     * @see SqlSyntaxUtils#enquoteIdentifier(String)
     */
    String quotedName() {
        return SqlSyntaxUtils.enquoteIdentifier(name);
    }

    /**
     * Generates the {@code CREATE TABLE} statement for this external table.
     * <p>
     * If {@link OutputResource#path()} of {@code outputResource} is empty, a placeholder, {@code '##REPLACE_ME##'}, is
     * inserted in the statement text as the external file path.
     * </p>
     *
     * @return {@code CREATE TABLE} statement for this table.
     */
    String toCreateTableStatement() {
        var sb = new StringBuilder("create table ").append(quotedName()).append(" external file ");
        outputResource.path().ifPresentOrElse(
                path -> sb.append(SqlSyntaxUtils.enquoteLiteral(path.toString())),
                () -> sb.append("'##REPLACE_ME##'"));
        sb.append(" (\n");
        int columnCount = columnCount();
        int lastColumn = columnCount - 1;
        for (int columnIdx = 0; columnIdx < columnCount; columnIdx++) {
            sb.append("  ");
            columns.get(columnIdx).appendColumnDefinition(sb);
            if (columnIdx != lastColumn) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(");\n");
        return sb.toString();
    }

    /**
     * Write {@code row} to {@code out}.
     *
     * @param row
     *         row data
     * @param out
     *         output stream to write
     * @throws IOException
     *         for errors writing to {@code out}
     */
    void writeRow(Row row, OutputStream out) throws IOException {
        /*
         The column under- or overflow (ignoring EndColumn) like documented in ADR 2023-02 is not handled here,
         this allows the behaviour to be pluggable in the caller of writeRow.
         
         Here we ignore overflow (NOTE if there is an end column, the last column is not written, but instead writes
         the end column value)
        */
        int rowColumnCount = Math.min(row.size(), columns.size());
        int currentColumn;
        for (currentColumn = 0; currentColumn < rowColumnCount; currentColumn++) {
            Column column = columns.get(currentColumn);
            column.writeValue(row.get(currentColumn), out);
        }
        // write underflow columns as empty, and/or EndColumn if present
        for (; currentColumn < columns.size(); currentColumn++) {
            Column column = columns.get(currentColumn);
            column.writeEmpty(out);
        }
    }

    /**
     * Derives an external table from the content of a {@link CsvFile}.
     *
     * @param csvFile
     *         CSV file instance
     * @param tableConfig
     *         external table configuration
     * @param rowProcessors
     *         additional row processors
     * @return external table
     */
    static ExternalTable deriveFrom(CsvFile csvFile, Config tableConfig, RowProcessor... rowProcessors) {
        var externalTableProcessor = new ExternalTableProcessor(tableConfig);
        for (RowProcessor rowProcessor : rowProcessors) {
            externalTableProcessor.subscribe(rowProcessor);
        }
        if (csvFile.readFile(externalTableProcessor) instanceof ProcessingResult.StopWithException swe) {
            throw new InvalidTableException("Could not derive external table", swe.exception());
        }
        return externalTableProcessor.getExternalTable();
    }

    /**
     * External table configuration.
     *
     * @param tableName
     *         table name (can be {@code null})
     * @param outputResource
     *         output resource of external table (can be {@code null}, but then no file can be created)
     * @param defaultEncoding
     *         default encoding to use for string columns
     * @param endColumnType
     *         type of end column ({@code NONE} to have no end column)
     * @param byteOrder
     *         byte order
     */
    record Config(String tableName, OutputResource outputResource, FbEncoding defaultEncoding,
            EndColumn.Type endColumnType, ByteOrderType byteOrder) {

        Config {
            if (outputResource == null) {
                outputResource = OutputResource.nullOutputResource();
            }
            requireNonNull(defaultEncoding, "defaultEncoding");
            requireNonNull(endColumnType, "endColumnType");
        }

        Config(String tableName, Path externalTableFile, FbEncoding defaultEncoding, EndColumn.Type endColumnType,
                ByteOrderType byteOrder) {
            this(tableName, OutputResource.of(externalTableFile), defaultEncoding, endColumnType, byteOrder);
        }

        Optional<EndColumn> endColumn() {
            return endColumnType.getEndColumn();
        }

    }

}
