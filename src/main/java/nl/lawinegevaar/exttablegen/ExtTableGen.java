// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;

import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

/**
 * Core of ext-table-gen which provides the necessary action of deriving and generating external tables.
 */
class ExtTableGen {

    private static final System.Logger log = System.getLogger(ExtTableGen.class.getName());

    private EtgConfig config;
    private ExternalTable externalTable;

    private ExtTableGen(EtgConfig config) {
        this.config = requireNonNull(config, "config");
    }

    EtgConfig config() {
        return config;
    }

    /**
     * Creates a {@link CsvFile} instance.
     *
     * @return CsvFile instance
     */
    CsvFile getCsvFile() {
        return config.inputConfig()
                .map(inputConfig -> {
                    log.log(INFO, "Reading CSV input from ''{0}''", inputConfig.path());
                    return inputConfig.toCsvFile();
                })
                .orElseThrow(() -> new MissingInputResourceException("ExtTableGen config has no input configuration"));
    }

    /**
     * Gets the external table from the current config, throwing an exception if the current config is invalid.
     *
     * @return external table
     * @see #getOrDeriveExternalTable()
     * @see #deriveExternalTable()
     */
    ExternalTable requireExternalTable() {
        if (externalTable != null) return externalTable;
        return this.externalTable = config.tableConfig().toExternalTable();
    }

    /**
     * Gets the external table from the current config.
     * <p>
     * If the current config does not result in a valid external table, the input file is used to derive an external
     * table.
     * </p>
     *
     * @return external table
     * @see #requireExternalTable()
     * @see #deriveExternalTable()
     */
    ExternalTable getOrDeriveExternalTable() {
        try {
            return requireExternalTable();
        } catch (RuntimeException e) {
            return deriveExternalTable();
        }
    }

    /**
     * Derive the external table definition.
     * <p>
     * The config of this instance is updated with the table config derived from the returned external table.
     * </p>
     *
     * @return external table definition
     * @see #requireExternalTable()
     * @see #getOrDeriveExternalTable()
     */
    ExternalTable deriveExternalTable() {
        CsvFile csvFile = getCsvFile();
        TableDerivationConfig tableDerivationConfig = config.tableDerivationConfig();
        ExternalTable externalTable = ExternalTable.deriveFrom(
                csvFile.withConfig(
                        // Require all rows to have same number of columns
                        csvFile.config().withBuilderCustomizer(
                                b -> b.withRowValidator(ColumnCountValidator.fromFirstRow()))),
                new ExternalTable.Config(config.tableConfig().name(), createExternalTableOutputResource(),
                        tableDerivationConfig.columnEncoding(), tableDerivationConfig.endColumnType()),
                new StopOnExceptionProcessor(CsvValidationException.class));

        config = config.withTableConfig(cfg -> TableConfig.of(externalTable).withOutputConfig(cfg.outputConfig()));
        
        return this.externalTable = externalTable;
    }

    /**
     * Creates an output resource for the external table data from the <i>output</i> options.
     *
     * @return output resource for writing external table data
     */
    private OutputResource createExternalTableOutputResource() {
        return config.tableConfig().outputConfig()
                .map(OutputConfig::toOutputResource)
                .orElseGet(OutputResource::nullOutputResource);
    }

    /**
     * Reads the CSV file and writes the data to the output resource of the external table.
     */
    void writeExternalTable() {
        ExternalTable externalTable = getOrDeriveExternalTable();
        log.log(INFO, "Writing external table to ''{0}''",
                externalTable.outputResource().path().map(String::valueOf).orElse("{no name)"));
        CsvFile csvFile = getCsvFile();
        CsvFile.Config originalConfig = csvFile.config();
        csvFile = csvFile.withConfig(originalConfig.withBuilderCustomizer(
                b -> {
                    b.withRowValidator(ColumnCountValidator.of(externalTable));
                    // Not really needed when using ExternalTable directly derived from CsvFile
                    b.withRowValidator(DelayedRowValidator
                            .delay(ColumnSizeValidator.of(externalTable))
                            .untilAfterRow(originalConfig.hasHeaderRow() ? 1 : 0));
                }));
        try (var tableWriter = new ExternalTableWriter(externalTable)) {
            var multiplexer = new MultiplexRowProcessor(tableWriter,
                    new StopOnExceptionProcessor(CsvValidationException.class));
            ProcessingResult result = csvFile.readFile(multiplexer);

            if (result instanceof ProcessingResult.StopWithException we) {
                Exception exception = we.exception();
                if (exception instanceof ExtTableGenException etge) {
                    throw etge;
                }
                throw new InvalidTableException("An exception occurred while writing the external table", exception);
            }
            log.log(INFO, "Finished writing external table");
        } catch (IOException e) {
            throw new InvalidTableException("An exception occurred while writing an external table", e);
        }
    }

    static ExtTableGen of(EtgConfig etgConfig) {
        var etg = new ExtTableGen(etgConfig);
        switch (etgConfig.tableDerivationConfig().mode()) {
        case NEVER -> etg.requireExternalTable();
        case ALWAYS -> etg.deriveExternalTable();
        case INCOMPLETE -> etg.getOrDeriveExternalTable();
        }
        return etg;
    }

}

