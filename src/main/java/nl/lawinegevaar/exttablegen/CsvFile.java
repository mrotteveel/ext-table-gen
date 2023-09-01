// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.Reader;
import java.lang.System.Logger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.System.Logger.Level.TRACE;
import static java.util.Objects.requireNonNull;

/**
 * Reads rows from a CSV file, and pushes the rows to a {@link RowProcessor} for further processing.
 */
final class CsvFile {

    private static final Logger log = System.getLogger(CsvFile.class.getName());

    private final InputResource inputResource;
    private final Config config;

    /**
     * Creates a {@code CsvFile}.
     * <p>
     * If {@code inputResource} is not repeatable (i.e. it can only be read once), then the caller is responsible for
     * creating new instances when needed, e.g. using {@link #withInputResource(InputResource)})
     * </p>
     *
     * @param inputResource
     *         input resource for reading CSV
     * @param config
     *         configuration for the CSV file
     */
    CsvFile(InputResource inputResource, Config config) {
        this.inputResource = requireNonNull(inputResource, "inputResource");
        this.config = requireNonNull(config, "config");
    }

    /**
     * Returns a copy of this {@code CsvFile} with the new {@code config}.
     *
     * @param config
     *         configuration for the CSV file
     * @return new CSV file instance with the same input resource, but new config
     */
    CsvFile withConfig(Config config) {
        return new CsvFile(inputResource, config);
    }

    /**
     * Returns a copy of the {@code CsvFile} with the new {@code inputResource}.
     *
     * @param inputResource
     *         input resource
     * @return new CSV file instance with the same config, but new input resource
     */
    CsvFile withInputResource(InputResource inputResource) {
        return new CsvFile(inputResource, config);
    }

    /**
     * @return the configuration of this CSV file
     */
    Config config() {
        return config;
    }

    /**
     * Reads the file, pushing header, rows, exceptions and completion to {@link RowProcessor}.
     *
     * @param rowProcessor
     *         row processor to accept the rows and other information
     * @return processing result, which is either the {@code Stop} or {@code Unsubscribe} from the processor which ended
     * processing prematurely, or a {@code Done} at the end of all processing
     * @throws FatalRowProcessingException
     *         for fatal {@code Exception}s or when a &mdash; possible &mdash; infinite loop in processing is detected
     */
    ProcessingResult readFile(RowProcessor rowProcessor) throws FatalRowProcessingException {
        try (var reader = inputResource.newReader(config.charset());
             CSVReader csvReader = createCSVReader(reader)) {
            long lastExceptionLine = -1;

            Row header;
            if (config.headerRow) {
                try {
                    String[] data = csvReader.readNext();
                    if (data == null) return ProcessingResult.done();
                    header = new Row(csvReader.getLinesRead(), List.of(data));
                } catch (CsvException e) {
                    lastExceptionLine = csvReader.getLinesRead();
                    ProcessingResult headerResult = fireOnException(rowProcessor, e);
                    if (haltOnProcessingResult(headerResult)) {
                        return headerResult;
                    }
                    header = Row.noHeader();
                }
            } else {
                header = Row.noHeader();
            }
            ProcessingResult headerResult = rowProcessor.onHeader(header);
            if (haltOnProcessingResult(headerResult)) {
                return headerResult;
            }

            while (true) {
                try {
                    String[] data = csvReader.readNext();
                    if (data == null) return ProcessingResult.done();

                    ProcessingResult rowResult = rowProcessor.onRow(new Row(csvReader.getLinesRead(), List.of(data)));
                    if (haltOnProcessingResult(rowResult)) {
                        return rowResult;
                    }
                } catch (CsvException e) {
                    long currentLinesRead = csvReader.getLinesRead();
                    if (lastExceptionLine == currentLinesRead) {
                        // Guard against infinite loops if we cannot move forward.
                        // NOTE: unclear if this can happen in practice for CsvException.
                        var secondErrorOnLine = new FatalRowProcessingException(
                                ("Processing was terminated as a second exception occurred on line %d, which could "
                                 + "cause an infinite loop").formatted(currentLinesRead), e);
                        ProcessingResult result = fireOnException(rowProcessor, secondErrorOnLine);
                        // NOTE: We're ignoring the advice (e.g. Continue) of the processing result
                        if (result instanceof ProcessingResult.StopWithException swe) {
                            secondErrorOnLine.addSuppressed(swe.exception());
                        }
                        throw secondErrorOnLine;
                    }

                    ProcessingResult onExceptionResult = fireOnException(rowProcessor, e);
                    if (haltOnProcessingResult(onExceptionResult)) {
                        return onExceptionResult;
                    }

                    lastExceptionLine = currentLinesRead;
                    csvReader.readNextSilently();
                }
            }
        } catch (Exception e) {
            // Other exceptions than CsvException are likely not recoverable
            var fdce = new FatalRowProcessingException("Processing was terminated due to a fatal exception", e);
            ProcessingResult result = fireOnException(rowProcessor, fdce);
            if (result instanceof ProcessingResult.StopWithException swe) {
                fdce.addSuppressed(swe.exception());
            }
            throw fdce;
        } finally {
            ProcessingResult result = fireOnComplete(rowProcessor);
            if (result instanceof ProcessingResult.StopWithException swe) {
                log.log(TRACE, "Received an exception from onComplete", swe.exception());
            }
        }
    }

    /**
     * Checks the processing result and logs basic information if it needs to stop.
     *
     * @param result
     *         processing result
     * @return {@code true} when processing needs to halt, {@code false} to continue
     */
    private boolean haltOnProcessingResult(ProcessingResult result) {
        if (result instanceof ProcessingResult.Stop) {
            log.log(TRACE, "Received Stop result");
            return true;
        } else if (result instanceof ProcessingResult.Unsubscribe) {
            log.log(TRACE, "Received Unsubscribe result (interpreted as stop signal)");
            return true;
        }

        return false;
    }

    /**
     * Send an exception to the row processor, and handles the return value or exception from the row processor.
     * <p>
     * If the result from the processor is a plain {@code Stop}, a {@code StopWithException} is returned with
     * {@code exception}. If the result is a {@code StopWithException}, {@code exception} is added as a suppressed
     * exception. Exceptions thrown from the row processor result in a {@code StopWithException} (with {@code exception}
     * added as a suppressed exception)
     * </p>
     *
     * @param rowProcessor
     *         row processor
     * @param exception
     *         exception to send to the row processor
     * @return processing result received from the processor, or converted from a received exception
     */
    private ProcessingResult fireOnException(RowProcessor rowProcessor, Exception exception) {
        try {
            ProcessingResult processingResult = rowProcessor.onException(exception);
            if (processingResult instanceof ProcessingResult.StopWithException swe) {
                swe.exception().addSuppressed(exception);
            } else if (processingResult instanceof ProcessingResult.Stop) {
                return ProcessingResult.stopWith(exception);
            }
            return processingResult;
        } catch (RuntimeException e) {
            e.addSuppressed(exception);
            return ProcessingResult.stopWith(e);
        }
    }

    /**
     * Sends a <em>complete</em> signal to the row processor, and handles the return value or exception from the row
     * processor.
     *
     * @param rowProcessor
     *         row processor
     * @return processing result received from the processor, or converted from a received exception
     */
    private ProcessingResult fireOnComplete(RowProcessor rowProcessor) {
        try {
            return rowProcessor.onComplete();
        } catch (RuntimeException e) {
            return ProcessingResult.stopWith(e);
        }
    }

    private CSVReader createCSVReader(Reader reader) {
        var builder = new CSVReaderBuilder(reader);
        config.builderCustomizer.accept(builder);
        return builder
                .withCSVParser(config.parserConfig.createParser())
                .withSkipLines(config.skipLines)
                .build();
    }

    @Override
    public String toString() {
        return "CsvFile{" +
               "inputResource=" + inputResource +
               ", config=" + config +
               '}';
    }

    /**
     * Configuration for reading CSV files.
     *
     * @param charset
     *         character set for reading the file
     * @param skipLines
     *         number of lines to skip before reading header or first row (primarily for testing purposes)
     * @param headerRow
     *         {@code true} read the first line after {@code skipLines} as header, {@code false} read the first line
     *         after {@code skipLines} as row data
     * @param parserConfig
     *         CSV parser config
     * @param builderCustomizer
     *         additional customization of the CSV reader builder
     * @see #Config(Charset, int, boolean, CsvParserConfig)
     */
    record Config(Charset charset, int skipLines, boolean headerRow, CsvParserConfig parserConfig,
            Consumer<CSVReaderBuilder> builderCustomizer) {

        private static final Consumer<CSVReaderBuilder> VOID_CUSTOMIZER = i -> {
        };

        Config {
            requireNonNull(charset, "charset");
            requireNonNull(parserConfig, "parserConfig");
            requireNonNull(builderCustomizer, "builderCustomizer");
        }

        /**
         * @param charset
         *         character set for reading the file
         * @param skipLines
         *         number of lines to skip before reading first header or row (primarily for testing purposes)
         * @param headerRow
         *         {@code true} read the first line after {@code skipLines} as header, {@code false} read the first line
         *         after {@code skipLines} as row data
         * @param parserConfig
         *         CSV parser config
         */
        Config(Charset charset, int skipLines, boolean headerRow, CsvParserConfig parserConfig) {
            this(charset, skipLines, headerRow, parserConfig, VOID_CUSTOMIZER);
        }

        /**
         * Returns a copy of this {@code Config} with the new {@code builderCustomizer}.
         * <p>
         * If the existing builder customizer is to be retained, then the caller is responsible for calling it within
         * the new builder customizer.
         * </p>
         *
         * @param builderCustomizer
         *         new builder customizer
         * @return new {@code Config} derived from this instance with {@code builderCustomizer} replaced
         */
        Config withBuilderCustomizer(Consumer<CSVReaderBuilder> builderCustomizer) {
            return new Config(charset, skipLines, headerRow, parserConfig, builderCustomizer);
        }

    }

}
