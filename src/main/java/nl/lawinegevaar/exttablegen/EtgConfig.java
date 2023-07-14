// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbEncoding;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/**
 * Configuration for an ExtTableGen run.
 *
 * @param tableConfig
 *         table configuration
 * @param tableDerivationConfig
 *         configuration to be used when deriving a new external table
 * @param csvFileConfig
 *         optional CSV file configuration
 */
record EtgConfig(TableConfig tableConfig, TableDerivationConfig tableDerivationConfig,
        Optional<CsvFileConfig> csvFileConfig) {

    EtgConfig {
        requireNonNull(tableConfig, "tableConfig");
        requireNonNull(tableDerivationConfig, "tableDerivationConfig");
        requireNonNull(csvFileConfig, "csvFileConfig");
    }

    EtgConfig(TableConfig tableConfig, TableDerivationConfig tableDerivationConfig, CsvFileConfig csvFileConfig) {
        this(tableConfig, tableDerivationConfig, Optional.ofNullable(csvFileConfig));
    }

    EtgConfig withTableConfig(TableConfig tableConfig) {
        if (this.tableConfig.equals(tableConfig)) return this;
        return new EtgConfig(tableConfig, tableDerivationConfig, csvFileConfig);
    }

    EtgConfig withTableConfig(UnaryOperator<TableConfig> tableConfigGenerator) {
        return withTableConfig(tableConfigGenerator.apply(tableConfig));
    }

    EtgConfig withTableDerivationConfig(TableDerivationConfig tableDerivationConfig) {
        if (this.tableDerivationConfig.equals(tableDerivationConfig)) return this;
        return new EtgConfig(tableConfig, tableDerivationConfig, csvFileConfig);
    }

    EtgConfig withTableDerivationConfig(UnaryOperator<TableDerivationConfig> tableDerivationConfigGenerator) {
        return withTableDerivationConfig(tableDerivationConfigGenerator.apply(tableDerivationConfig));
    }

    EtgConfig withCsvFileConfig(CsvFileConfig csvFileConfig) {
        if (Objects.equals(this.csvFileConfig.orElse(null), csvFileConfig)) return this;
        return new EtgConfig(tableConfig, tableDerivationConfig, csvFileConfig);
    }

    /**
     * Create a new {@code EtgConfig} with an updated {@code csvFileConfig}.
     *
     * @param csvFileConfigGenerator
     *         function to convert the existing CSV file config to a new CSV file config (NOTE: if the returned value is
     *         {@code null}, the {@code csvFileConfigCreator} will be called)
     * @param csvFileConfigCreator
     *         function to create a new CSV file config (called if there is no current CSV file config, or if
     *         {@code csvFileConfigGenerator} returned {@code null}). This function can return {@code null} to set an
     *         empty CSV file config
     * @return new {@code EtgConfig} with its {@code csvFileConfig} replaced
     */
    EtgConfig withCsvFileConfig(
            UnaryOperator<CsvFileConfig> csvFileConfigGenerator, Supplier<CsvFileConfig> csvFileConfigCreator) {
        return withCsvFileConfig(csvFileConfig.map(csvFileConfigGenerator).orElseGet(csvFileConfigCreator));
    }

}

/**
 * CSV file configuration.
 *
 * @param path
 *         path of the CSV file
 * @param charset
 *         character set of the CSV file
 * @param headerRow
 *         CSV file has a header row
 */
record CsvFileConfig(Path path, Charset charset, boolean headerRow) {

    CsvFileConfig {
        requireNonNull(path);
        requireNonNull(charset);
    }

    CsvFileConfig(String path, String charset, boolean headerRow) {
        this(Path.of(path), Charset.forName(charset), headerRow);
    }

    /**
     * Converts this CSV file config to an {@link InputResource}.
     *
     * @return input resource
     */
    InputResource toInputResource() {
        return InputResource.of(path);
    }

    /**
     * Converts this CSV file config to a {@link CsvFile}.
     *
     * @return csv file instance
     */
    CsvFile toCsvFile() {
        return new CsvFile(toInputResource(), new CsvFile.Config(charset, 0, headerRow));
    }

    CsvFileConfig withPath(Path path) {
        if (this.path.equals(path)) return this;
        return new CsvFileConfig(path, charset, headerRow);
    }

    CsvFileConfig withCharset(Charset charset) {
        if (this.charset.equals(charset)) return this;
        return new CsvFileConfig(path, charset, headerRow);
    }

    CsvFileConfig withHeaderRow(boolean headerRow) {
        if (this.headerRow == headerRow) return this;
        return new CsvFileConfig(path, charset, headerRow);
    }

}

/**
 * External table file.
 *
 * @param path
 *         path of the external table file
 * @param overwrite
 *         {@code true} ext-table-gen can overwrite file if it already exists
 */
record TableFile(Path path, boolean overwrite) {

    TableFile(String path, boolean overwrite) {
        this(Path.of(path), overwrite);
    }

    /**
     * Converts this table file to an {@link OutputResource}.
     *
     * @return output resource
     */
    OutputResource toOutputResource() {
        return OutputResource.of(path, overwrite);
    }

}

/**
 * A &mdash; potentially &mdash; invalid or incomplete external table configuration.
 *
 * @param name
 *         name of the table (can be {@code null})
 * @param columns
 *         list of columns (can be empty, {@code null} will be replaced with empty list)
 * @param tableFile
 *         (optional) table file
 * @param byteOrder
 *         byte order
 */
record TableConfig(String name, List<Column> columns, Optional<TableFile> tableFile, ByteOrderType byteOrder) {

    private static final TableConfig EMPTY = new TableConfig(null, null, Optional.empty(), ByteOrderType.AUTO);

    TableConfig {
        columns = columns != null ? List.copyOf(columns) : List.of();
        if (byteOrder == null) {
            byteOrder = ByteOrderType.AUTO;
        }
    }

    TableConfig(String name, List<Column> columns, TableFile tableFile, ByteOrderType byteOrder) {
        this(name, columns, Optional.ofNullable(tableFile), byteOrder);
    }

    /**
     * Returns an empty table config (no name, no column, no table file).
     *
     * @return empty table config
     */
    static TableConfig empty() {
        return EMPTY;
    }

    /**
     * Converts this config to an external table.
     *
     * @return external table
     * @throws IllegalArgumentException
     *         if this configuration is incomplete or invalid for an external table
     */
    ExternalTable toExternalTable() {
        return new ExternalTable(name(), columns(), tableFile.map(TableFile::toOutputResource).orElse(null), byteOrder);
    }

    Optional<String> toDdl() {
        try {
            return Optional.of(toExternalTable().toCreateTableStatement());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * @return value of {@link TableFile#overwrite()} or {@code false} if {@code tableFile} is empty
     */
    boolean allowTableFileOverwrite() {
        return tableFile.map(TableFile::overwrite).orElse(Boolean.FALSE);
    }

    TableConfig withName(String name) {
        if (Objects.equals(this.name, name)) return this;
        return new TableConfig(name, columns, tableFile, byteOrder);
    }

    TableConfig withColumns(List<Column> columns) {
        if (this.columns.equals(columns) || this.columns.isEmpty() && columns == null) return this;
        return new TableConfig(name, columns, tableFile, byteOrder);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    TableConfig withTableFile(Optional<TableFile> tableFile) {
        return withTableFile(tableFile.orElse(null));
    }

    TableConfig withTableFile(TableFile tableFile) {
        if (Objects.equals(this.tableFile.orElse(null), tableFile)) return this;
        return new TableConfig(name, columns, tableFile, byteOrder);
    }

    TableConfig withByteOrder(ByteOrderType byteOrder) {
        if (this.byteOrder == byteOrder) return this;
        return new TableConfig(name, columns, tableFile, byteOrder);
    }

    static TableConfig of(ExternalTable externalTable) {
        return new TableConfig(
                externalTable.name(),
                externalTable.columns(),
                externalTable.outputResource().path().map(path -> new TableFile(path, false)),
                externalTable.byteOrder());
    }

}

/**
 * Table derivation config.
 * <p>
 * These configuration options are only used when deriving a new external table.
 * </p>
 *
 * @param columnEncoding
 *         column encoding (will use {@code ISO8859_1} when {@code null})
 * @param endColumnType
 *         end column type (will use {@code LF} when {@code null})
 * @param mode
 *         mode of table derivation is applied
 */
record TableDerivationConfig(FbEncoding columnEncoding, EndColumn.Type endColumnType, TableDerivationMode mode) {

    static final FbEncoding DEFAULT_COLUMN_ENCODING = FbEncoding.ISO8859_1;
    static final EndColumn.Type DEFAULT_END_COLUMN_TYPE = EndColumn.Type.LF;
    // NOTE: Not defining a constant for default TableDerivationMode, as it is context specific

    private static final TableDerivationConfig DEFAULT_CONFIG =
            new TableDerivationConfig(DEFAULT_COLUMN_ENCODING, DEFAULT_END_COLUMN_TYPE, TableDerivationMode.INCOMPLETE);

    TableDerivationConfig {
        columnEncoding = requireNonNullElse(columnEncoding, DEFAULT_COLUMN_ENCODING);
        endColumnType = requireNonNullElse(endColumnType, DEFAULT_END_COLUMN_TYPE);
        mode = requireNonNullElse(mode, TableDerivationMode.INCOMPLETE);
    }

    TableDerivationConfig withColumnEncoding(FbEncoding columnEncoding) {
        return new TableDerivationConfig(columnEncoding, endColumnType, mode);
    }

    TableDerivationConfig withEndColumnType(EndColumn.Type endColumnType) {
        return new TableDerivationConfig(columnEncoding, endColumnType, mode);
    }

    TableDerivationConfig withMode(TableDerivationMode mode) {
        return new TableDerivationConfig(columnEncoding, endColumnType, mode);
    }

    static TableDerivationConfig getDefault() {
        return DEFAULT_CONFIG;
    }

}

/**
 * Initialization behaviour for the external table.
 */
enum TableDerivationMode {
    /**
     * Never derive the external table, always use the current configuration.
     * <p>
     * Fails if the current configuration is incomplete or invalid.
     * </p>
     */
    NEVER,
    /**
     * Always derive the external table, even if the current configuration defines a valid external table.
     */
    ALWAYS,
    /**
     * Only derive the external table when the current configuration is incomplete.
     */
    INCOMPLETE
}