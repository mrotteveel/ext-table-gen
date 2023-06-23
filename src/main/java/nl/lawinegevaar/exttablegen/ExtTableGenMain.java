// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import jakarta.xml.bind.JAXBException;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.System.Logger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.LogManager;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Objects.requireNonNullElse;

/**
 * Main class for running ext-table-gen from the command line.
 */
@CommandLine.Command(name= "ext-table-gen", mixinStandardHelpOptions = true, sortOptions = false)
class ExtTableGenMain implements Runnable {

    private static final Logger log;
    static {
        if (System.getProperty("java.util.logging.config.file") == null) {
            try (var in = ExtTableGenMain.class.getResourceAsStream("default-logging.properties")) {
                LogManager.getLogManager().readConfiguration(in);
            } catch (IOException ignored) {
                // fallback to Java default
            }
        }
        log = System.getLogger(ExtTableGenMain.class.getName());
    }

    private static final FbEncoding DEFAULT_COLUMN_ENCODING = FbEncoding.ISO8859_1;
    private static final EndColumn.Type DEFAULT_END_COLUMN_TYPE = EndColumn.Type.LF;
    private static final TableDerivationMode DEFAULT_TABLE_DERIVATION_MODE = TableDerivationMode.INCOMPLETE;
    private static final Charset DEFAULT_INPUT_CHARSET = StandardCharsets.UTF_8;

    @CommandLine.Option(names = "--input-file", paramLabel = "CSV",
            description = "CSV input file (RFC 4180 format)", order = 100)
    Path inputFile;

    @CommandLine.Option(names = "--input-charset", paramLabel = "CHARSET",
            description = "Character set of the input file (Java character set name). Default: UTF-8", order = 110)
    Charset inputCharset;

    @CommandLine.Option(names = "--input-header", negatable = true, fallbackValue = "true",
            description = "First row of input file is a header. Default: true", order = 120)
    Boolean inputHeader;

    @CommandLine.Option(names = "--output-file", paramLabel = "FILE", description = "External table output file",
            order = 200)
    Path outputFile;

    @CommandLine.Option(names = "--overwrite-output", negatable = true, fallbackValue = "true",
            description = "Overwrite the output file if it already exists. Default: false", order = 210)
    Boolean outputOverwrite;

    @CommandLine.Option(names = "--table-name", paramLabel = "TABLE",
            description = "Name of the external table", order = 300)
    String tableName;

    @CommandLine.Option(names = "--column-encoding", paramLabel = "ENCODING", converter = FbEncodingConverter.class,
            description = "Name of the character set of output columns (Firebird character set name). Default: "
                          + "ISO8859_1", order = 310)
    FbEncoding columnEncoding;

    @CommandLine.Option(names = "--end-column", paramLabel = "TYPE",
            description = "The type of end column (LF, CRLF or NONE). Default: LF", order = 320)
    EndColumn.Type endColumnType;

    @CommandLine.Option(names = "--table-derivation-mode", paramLabel = "MODE",
            description = "When to (re)generate the table definition ({INCOMPLETE | ALWAYS}). Default: INCOMPLETE",
            order = 330)
    TableDerivationMode tableDerivationMode;

    @CommandLine.Option(names = "--config-in", paramLabel = "FILE",
            description = "Configuration file to read (command-line options take precedence)", order = 400)
    Path configIn;

    @CommandLine.Option(names = "--config-out", paramLabel = "FILE",
            description = "Configuration file to write", order = 410)
    Path configOut;

    @CommandLine.Option(names = "--overwrite-config", negatable = true, fallbackValue = "true",
            description = "Overwrite the config file (--config-out) if it already exists. Default: false", order = 420)
    Boolean configOverwrite;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    private final ConfigMapper configMapper = new ConfigMapper();

    public static void main(String[] args) {
        System.exit(
                parseAndExecute(args));
    }

    static int parseAndExecute(String... args) {
        return new CommandLine(new ExtTableGenMain()).execute(args);
    }

    @Override
    public void run() {
        EtgConfig etgConfig = readConfigFile()
                .map(this::mergeConfig)
                .orElseGet(this::createConfig);
        validate(etgConfig);
        ExtTableGen etg = ExtTableGen.of(etgConfig);
        writeConfigFile(etg.config());
        etg.writeExternalTable();
    }

    /**
     * Validates if {@code etgConfig} has the required options set.
     *
     * @param etgConfig
     *         external-table-gen configuration
     */
    private void validate(EtgConfig etgConfig) {
        if (etgConfig.inputConfig().map(InputConfig::path).isEmpty()) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Missing option: --input-file.");
        }
        if (etgConfig.inputConfig().map(InputConfig::charset).isEmpty()) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Missing option: --input-charset.");
        }
        if (etgConfig.tableConfig().outputConfig().map(OutputConfig::path).isEmpty()) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Missing option: --output-file.");
        }
    }

    /**
     * Merges the command-line parameters with {@code config} to a new configuration.
     * <p>
     * The command-line parameters take precedence, but some parameters require {@code --table-derivation-mode=ALWAYS}
     * to effect a change (e.g. {@code --column-encoding} and {@code --end-column-type}) if the config already defines
     * a valid table definition
     * </p>
     *
     * @param config
     *         the initial external-table-gen configuration
     * @return the external-table-gen configuration after merging (can be {@code config} if there was nothing to merge)
     */
    private EtgConfig mergeConfig(EtgConfig config) {
        if (outputFile != null) {
            config = config.withTableConfig(
                    cfg -> cfg.withOutputConfig(new OutputConfig(
                            outputFile, requireNonNullElse(outputOverwrite, cfg.allowOutputConfigOverwrite()))));
        }

        if (tableName != null) {
            config = config.withTableConfig(cfg -> cfg.withName(tableName));
        }

        if (columnEncoding != null || endColumnType != null || tableDerivationMode != null) {
            config = config.withTableDerivationConfig(
                    cfg -> {
                        TableDerivationConfig derivationConfig = cfg;
                        if (columnEncoding != null) {
                            derivationConfig = derivationConfig.withColumnEncoding(columnEncoding);
                        }
                        if (endColumnType != null) {
                            derivationConfig = derivationConfig.withEndColumnType(endColumnType);
                        }
                        if (tableDerivationMode != null) {
                            derivationConfig = derivationConfig.withMode(tableDerivationMode);
                        }
                        return derivationConfig;
                    });
        }

        config = config.withInputConfig(
                cfg -> {
                    InputConfig inputConfig = cfg;
                    if (inputFile != null) {
                        inputConfig = inputConfig.withPath(inputFile);
                    }
                    if (inputCharset != null) {
                        inputConfig = inputConfig.withCharset(inputCharset);
                    }
                    if (inputHeader != null) {
                        inputConfig = inputConfig.withHasHeaderRow(inputHeader);
                    }
                    return inputConfig;
                },
                this::createInputConfig);

        return config;
    }

    /**
     * Creates an external-table-gen configuration from the command-line options, without using {@link #configIn}.
     *
     * @return external-table-gen configuration
     */
    private EtgConfig createConfig() {
        return new EtgConfig(
                new TableConfig(tableName, List.of(),
                        Optional.ofNullable(outputFile)
                                .map(file -> new OutputConfig(file, outputOverwriteOrDefault()))),
                new TableDerivationConfig(
                        columnEncodingOrDefault(), endColumnTypeOrDefault(), tableDerivationModeOrDefault()),
                createInputConfig());
    }

    private InputConfig createInputConfig() {
        return inputFile != null ? new InputConfig(inputFile, inputCharsetOrDefault(), inputHeaderOrDefault()) : null;
    }

    Charset inputCharsetOrDefault() {
        return requireNonNullElse(inputCharset, DEFAULT_INPUT_CHARSET);
    }

    boolean inputHeaderOrDefault() {
        return requireNonNullElse(inputHeader, Boolean.TRUE);
    }

    private boolean outputOverwriteOrDefault() {
        return requireNonNullElse(outputOverwrite, Boolean.FALSE);
    }

    private FbEncoding columnEncodingOrDefault() {
        return requireNonNullElse(columnEncoding, DEFAULT_COLUMN_ENCODING);
    }

    private EndColumn.Type endColumnTypeOrDefault() {
        return requireNonNullElse(endColumnType, DEFAULT_END_COLUMN_TYPE);
    }

    private TableDerivationMode tableDerivationModeOrDefault() {
        return requireNonNullElse(tableDerivationMode, DEFAULT_TABLE_DERIVATION_MODE);
    }

    boolean configOverwriteOrDefault() {
        return requireNonNullElse(configOverwrite, Boolean.FALSE);
    }

    /**
     * Creates an output resource for the configuration to <em>write</em> from the <i>config</i> options.
     *
     * @return output resource for writing a configuration file
     */
    private OutputResource createConfigurationOutputResource() {
        return OutputResource.of(configOut, configOverwriteOrDefault());
    }

    /**
     * Writes the configuration from {@code etgConfig} to the location specified in the <i>config</i> options.
     * <p>
     * Failures to write are logged, not thrown out of this method.
     * </p>
     *
     * @param etgConfig
     *         external table configuration
     */
    private void writeConfigFile(EtgConfig etgConfig) {
        if (configOut != null) {
            log.log(INFO, "Writing configuration file ''{0}''", configOut);
            OutputResource outputResource = createConfigurationOutputResource();
            try (var out = outputResource.newOutputStream()) {
                configMapper.write(etgConfig, out);
            } catch (IOException | JAXBException e) {
                if (e instanceof FileAlreadyExistsException faee && !configOverwriteOrDefault()) {
                    log.log(WARNING,
                            "Configuration file ''{0}'' already exists and was not overwritten. Specify "
                            + "--overwrite-config to overwrite an existing configuration file", faee.getFile());
                } else {
                    log.log(ERROR, "Could not write configuration file", e);
                }
            }
        }
    }

    /**
     * Reads the configuration from the location specified in <i>config</i> options to a {@link EtgConfig} object.
     *
     * @return the configuration object, or empty if no config input file was specified
     * @throws InvalidConfigurationException
     *         if an error occurred reading or parsing the file
     */
    private Optional<EtgConfig> readConfigFile() {
        if (configIn != null) {
            log.log(INFO, "Reading configuration file ''{0}''", configIn);
            try (var in = Files.newInputStream(configIn)) {
                return Optional.of(configMapper.read(in));
            } catch (IOException | JAXBException e) {
                log.log(WARNING, "Could not read file ''{0}'': {1}", configIn, e.toString());
                throw new InvalidConfigurationException("Could not read configuration file", e);
            }
        }
        return Optional.empty();
    }

    /**
     * Converter to create {@link FbEncoding} from string.
     */
    static final class FbEncodingConverter implements CommandLine.ITypeConverter<FbEncoding> {

        @Override
        public FbEncoding convert(String value) {
            return FbEncoding.forName(value);
        }

    }

}
