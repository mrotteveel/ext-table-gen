// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import jakarta.xml.bind.JAXBException;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.LogManager;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;
import static nl.lawinegevaar.exttablegen.TableDerivationConfig.DEFAULT_COLUMN_ENCODING;
import static nl.lawinegevaar.exttablegen.TableDerivationConfig.DEFAULT_END_COLUMN_TYPE;

/**
 * Main class for running ext-table-gen from the command line.
 */
@CommandLine.Command(name= "ext-table-gen", mixinStandardHelpOptions = true, sortOptions = false,
        versionProvider = ExtTableGenMain.VersionProvider.class)
final class ExtTableGenMain implements Runnable {

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

    // Only default if no configuration file is used
    private static final TableDerivationMode DEFAULT_TABLE_DERIVATION_MODE = TableDerivationMode.INCOMPLETE;
    private static final Charset DEFAULT_CSV_CHARSET = StandardCharsets.UTF_8;

    @CommandLine.Option(names = "--csv-file", paramLabel = "CSV", description = "CSV file (RFC 4180 format)",
            order = 100)
    Path csvFile;

    @CommandLine.Option(names = "--csv-charset", paramLabel = "CHARSET",
            description = "Character set of the CSV file (Java character set name). Default: UTF-8", order = 110)
    Charset csvCharset;

    @CommandLine.Option(names = "--csv-header", negatable = true, fallbackValue = "true",
            description = "First row of CSV file is a header. Default: true", order = 120)
    Boolean csvHeader;

    @CommandLine.ArgGroup(exclusive = false)
    TableFileOptions tableFileOptions;

    static class TableFileOptions {
        @CommandLine.Option(names = "--table-file", required = true, paramLabel = "FILE",
                description = "External table file", order = 200)
        Path tableFilePath;

        @CommandLine.Option(names = "--overwrite-table-file", negatable = true, fallbackValue = "true",
                description = "Overwrite the table file if it already exists. Default: false", order = 210)
        Boolean overwriteTableFile;
    }

    @CommandLine.Option(names = "--table-name", paramLabel = "TABLE", description = "Name of the external table",
            order = 300)
    String tableName;

    @CommandLine.Option(names = "--byte-order", paramLabel = "ORDER",
            description = "Byte order ({LITTLE_ENDIAN | BIG_ENDIAN | AUTO}). Default: runtime byte order (effective "
                          + "value of AUTO)",
            order = 302)
    ByteOrderType byteOrder;

    @CommandLine.Option(names = "--column-encoding", paramLabel = "ENCODING", converter = FbEncodingConverter.class,
            description = "Name of the character set of external table columns (Firebird character set name). Default: "
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

    @CommandLine.ArgGroup(exclusive = false)
    ConfigOutOptions configOutOptions;

    static class ConfigOutOptions {
        @CommandLine.Option(names = "--config-out", required = true, paramLabel = "FILE",
                description = "Configuration file to write", order = 410)
        Path configOut;

        @CommandLine.Option(names = "--overwrite-config", negatable = true, fallbackValue = "true",
                description = "Overwrite the config file (--config-out) if it already exists. Default: false",
                order = 420)
        Boolean configOverwrite;
    }

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    private final ConfigMapper configMapper = new ConfigMapper();

    public static void main(String[] args) {
        System.exit(
                parseAndExecute(args));
    }

    static int parseAndExecute(String... args) {
        return new CommandLine(new ExtTableGenMain())
                .setExecutionExceptionHandler(new LogExceptionMessageHandler())
                .execute(args);
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
        var missingOptions = new ArrayList<String>(3);
        boolean missingCsvFile = etgConfig.csvFileConfig().map(CsvFileConfig::path).isEmpty();
        if (missingCsvFile) {
            missingOptions.add("--csv-file=CSV");
        } else if (etgConfig.csvFileConfig().map(CsvFileConfig::charset).isEmpty()) {
            // csv-charset is also empty if --csv-file wasn't specified and the config doesn't have <csvFile>;
            // this condition itself shouldn't happen in practice, unless there is some bug preventing fallback to the
            // default of UTF-8
            missingOptions.add("--csv-charset=CHARSET");
        }

        boolean missingTableFile = etgConfig.tableConfig().tableFile().map(TableFile::path).isEmpty();
        if (missingTableFile) {
            missingOptions.add("--table-file=FILE");
        }

        if (missingCsvFile || missingTableFile) {
            missingOptions.add("or --config-in=FILE");
        }
        
        if (!missingOptions.isEmpty()) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Missing option(s): " + String.join(", ", missingOptions));
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
        if (tableFileOptions != null && tableFileOptions.tableFilePath != null) {
            config = config.withTableConfig(
                    cfg -> cfg.withTableFile(new TableFile(
                            tableFileOptions.tableFilePath, overwriteTableFileOrDefault())));
        }

        if (tableName != null) {
            config = config.withTableConfig(cfg -> cfg.withName(tableName));
        }

        if (byteOrder != null) {
            config = config.withTableConfig(cfg -> cfg.withByteOrder(byteOrder));
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

        config = config.withCsvFileConfig(
                cfg -> {
                    CsvFileConfig csvFileConfig = cfg;
                    if (csvFile != null) {
                        csvFileConfig = csvFileConfig.withPath(csvFile);
                    }
                    if (csvCharset != null) {
                        csvFileConfig = csvFileConfig.withCharset(csvCharset);
                    }
                    if (csvHeader != null) {
                        csvFileConfig = csvFileConfig.withHeaderRow(csvHeader);
                    }
                    return csvFileConfig;
                },
                this::createCsvFileConfig);

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
                        Optional.ofNullable(tableFileOptions)
                                .map(opt -> tableFileOptions.tableFilePath)
                                .map(file -> new TableFile(file, overwriteTableFileOrDefault())),
                        byteOrderOrDefault()),
                new TableDerivationConfig(
                        columnEncodingOrDefault(), endColumnTypeOrDefault(), tableDerivationModeOrDefault()),
                createCsvFileConfig());
    }

    private CsvFileConfig createCsvFileConfig() {
        return csvFile != null ? new CsvFileConfig(csvFile, csvCharsetOrDefault(), csvHeaderOrDefault()) : null;
    }

    private Charset csvCharsetOrDefault() {
        return requireNonNullElse(csvCharset, DEFAULT_CSV_CHARSET);
    }

    boolean csvHeaderOrDefault() {
        return requireNonNullElse(csvHeader, Boolean.TRUE);
    }

    ByteOrderType byteOrderOrDefault() {
        return requireNonNullElseGet(byteOrder, ByteOrderType.AUTO::effectiveValue);
    }

    private Optional<TableFileOptions> tableFileOptions() {
        return Optional.ofNullable(tableFileOptions);
    }

    private boolean overwriteTableFileOrDefault() {
        return tableFileOptions()
                .map(opt -> opt.overwriteTableFile)
                .orElse(Boolean.FALSE);
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

    private Optional<ConfigOutOptions> configOutOptions() {
        return Optional.ofNullable(configOutOptions);
    }

    boolean configOverwriteOrDefault() {
        return configOutOptions().map(opt -> opt.configOverwrite).orElse(Boolean.FALSE);
    }

    /**
     * Creates an output resource for the configuration to <em>write</em> from the <i>config</i> options.
     *
     * @return output resource for writing a configuration file
     */
    private OutputResource createConfigurationOutputResource() {
        return OutputResource.of(configOutOptions().map(opt -> opt.configOut).orElseThrow(), configOverwriteOrDefault());
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
        Path configOut = configOutOptions().map(opt -> opt.configOut).orElse(null);
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

    static final class VersionProvider implements CommandLine.IVersionProvider {

        private static final String version;
        static {
            String tempVersion = "unknown version";
            try (InputStream in = VersionProvider.class.getResourceAsStream("version.properties")) {
                if (in != null) {
                    Properties props = new Properties();
                    props.load(in);
                    tempVersion = props.getProperty("ext-table-gen.version");
                }
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            } finally {
                version = tempVersion;
            }
        }

        @Override
        public String[] getVersion() {
            return new String[] {
                    "ext-table-gen %s - Firebird External Table Generator".formatted(version),
                    "Copyright 2023 Mark Rotteveel",
                    "Licensed under Apache 2.0, see https://www.apache.org/licenses/LICENSE-2.0 for full license"
            };
        }
    }

    static final class LogExceptionMessageHandler implements CommandLine.IExecutionExceptionHandler {

        @Override
        public int handleExecutionException(Exception ex, CommandLine cmd, CommandLine.ParseResult parseResult) {
            log.log(ERROR, ex.toString());
            log.log(DEBUG, "Exception terminating ext-table-gen", ex);
            return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : cmd.getCommandSpec().exitCodeOnExecutionException();
        }

    }
    
}
