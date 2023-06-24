// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

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
 * @param inputConfig
 *         optional input configuration
 */
record EtgConfig(TableConfig tableConfig, TableDerivationConfig tableDerivationConfig,
        Optional<InputConfig> inputConfig) {

    EtgConfig {
        requireNonNull(tableConfig, "tableConfig");
        requireNonNull(tableDerivationConfig, "tableDerivationConfig");
        requireNonNull(inputConfig, "inputConfig");
    }

    EtgConfig(TableConfig tableConfig, TableDerivationConfig tableDerivationConfig, InputConfig inputConfig) {
        this(tableConfig, tableDerivationConfig, Optional.ofNullable(inputConfig));
    }

    EtgConfig withTableConfig(TableConfig tableConfig) {
        if (this.tableConfig.equals(tableConfig)) return this;
        return new EtgConfig(tableConfig, tableDerivationConfig, inputConfig);
    }

    EtgConfig withTableConfig(UnaryOperator<TableConfig> tableConfigGenerator) {
        return withTableConfig(tableConfigGenerator.apply(tableConfig));
    }

    EtgConfig withTableDerivationConfig(TableDerivationConfig tableDerivationConfig) {
        if (this.tableDerivationConfig.equals(tableDerivationConfig)) return this;
        return new EtgConfig(tableConfig, tableDerivationConfig, inputConfig);
    }

    EtgConfig withTableDerivationConfig(UnaryOperator<TableDerivationConfig> tableDerivationConfigGenerator) {
        return withTableDerivationConfig(tableDerivationConfigGenerator.apply(tableDerivationConfig));
    }

    EtgConfig withInputConfig(InputConfig inputConfig) {
        if (Objects.equals(this.inputConfig.orElse(null), inputConfig)) return this;
        return new EtgConfig(tableConfig, tableDerivationConfig, inputConfig);
    }

    /**
     * Create a new {@code EtgConfig} with an updated {@code inputConfig}.
     *
     * @param inputConfigGenerator
     *         function to convert the existing input config to a new input config (NOTE: if the returned value is
     *         {@code null}, the {@code inputConfigCreator} will be called)
     * @param inputConfigCreator
     *         function to create a new input config (called if there is no current input config, or if
     *         {@code inputConfigGenerator} returned {@code null}). This function can return {@code null} to set an
     *         empty input config
     * @return new {@code EtgConfig} with its {@code inputConfig} replaced
     */
    EtgConfig withInputConfig(
            UnaryOperator<InputConfig> inputConfigGenerator, Supplier<InputConfig> inputConfigCreator) {
        return withInputConfig(inputConfig.map(inputConfigGenerator).orElseGet(inputConfigCreator));
    }

}

/**
 * Input file configuration.
 *
 * @param path
 *         path of the input file
 * @param charset
 *         character set of the input file
 * @param hasHeaderRow
 *         input file has a header row
 */
record InputConfig(Path path, Charset charset, boolean hasHeaderRow) {

    InputConfig {
        requireNonNull(path);
        requireNonNull(charset);
    }

    InputConfig(String path, String charset, boolean hasHeaderRow) {
        this(Path.of(path), Charset.forName(charset), hasHeaderRow);
    }

    /**
     * Converts this input config to an {@link InputResource}.
     *
     * @return input resource
     */
    InputResource toInputResource() {
        return InputResource.of(path);
    }

    /**
     * Converts this input config to a {@link CsvFile}.
     *
     * @return csv file instance
     */
    CsvFile toCsvFile() {
        return new CsvFile(toInputResource(), new CsvFile.Config(charset, 0, hasHeaderRow));
    }

    InputConfig withPath(Path path) {
        if (this.path.equals(path)) return this;
        return new InputConfig(path, charset, hasHeaderRow);
    }

    InputConfig withCharset(Charset charset) {
        if (this.charset.equals(charset)) return this;
        return new InputConfig(path, charset, hasHeaderRow);
    }

    InputConfig withHasHeaderRow(boolean hasHeaderRow) {
        if (this.hasHeaderRow == hasHeaderRow) return this;
        return new InputConfig(path, charset, hasHeaderRow);
    }

}

/**
 * Output file configuration.
 *
 * @param path
 *         path of the external table output file
 */
record OutputConfig(Path path, boolean allowOverwrite) {

    OutputConfig(String path, boolean allowOverwrite) {
        this(Path.of(path), allowOverwrite);
    }

    /**
     * Converts this output config to an {@link OutputResource}.
     *
     * @return output resource
     */
    OutputResource toOutputResource() {
        return OutputResource.of(path, allowOverwrite);
    }

}

/**
 * A &mdash; potentially &mdash; invalid or incomplete external table configuration.
 *
 * @param name
 *         name of the table (can be {@code null})
 * @param columns
 *         list of columns (can be empty, {@code null} will be replaced with empty list)
 * @param outputConfig
 *         (optional) output config
 */
record TableConfig(String name, List<Column> columns, Optional<OutputConfig> outputConfig) {

    TableConfig {
        columns = columns != null ? List.copyOf(columns) : List.of();
    }

    TableConfig(String name, List<Column> columns, OutputConfig outputConfig) {
        this(name, columns, Optional.ofNullable(outputConfig));
    }

    /**
     * Converts this config to an external table.
     *
     * @return external table
     * @throws IllegalArgumentException
     *         if this configuration is incomplete or invalid for an external table
     */
    ExternalTable toExternalTable() {
        return new ExternalTable(name(), columns(), outputConfig.map(OutputConfig::toOutputResource).orElse(null));
    }

    Optional<String> toDdl() {
        try {
            return Optional.of(toExternalTable().toCreateTableStatement());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * @return value of {@link OutputConfig#allowOverwrite()} or {@code false} if {@code outputConfig} is empty
     */
    boolean allowOutputConfigOverwrite() {
        return outputConfig.map(OutputConfig::allowOverwrite).orElse(Boolean.FALSE);
    }

    TableConfig withName(String name) {
        if (Objects.equals(this.name, name)) return this;
        return new TableConfig(name, columns, outputConfig);
    }

    TableConfig withColumns(List<Column> columns) {
        if (this.columns.equals(columns) || this.columns.isEmpty() && columns == null) return this;
        return new TableConfig(name, columns, outputConfig);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    TableConfig withOutputConfig(Optional<OutputConfig> outputConfig) {
        return withOutputConfig(outputConfig.orElse(null));
    }

    TableConfig withOutputConfig(OutputConfig outputConfig) {
        if (Objects.equals(this.outputConfig.orElse(null), outputConfig)) return this;
        return new TableConfig(name, columns, outputConfig);
    }

    static TableConfig of(ExternalTable externalTable) {
        return new TableConfig(externalTable.name(), externalTable.columns(),
                externalTable.outputResource().path().map(path -> new OutputConfig(path, false)));
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