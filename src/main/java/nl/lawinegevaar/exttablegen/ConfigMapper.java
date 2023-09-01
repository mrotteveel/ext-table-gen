// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import nl.lawinegevaar.exttablegen.convert.AbstractParseIntegralNumber;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseBigDecimal;
import nl.lawinegevaar.exttablegen.convert.ParseDatetime;
import nl.lawinegevaar.exttablegen.type.*;
import nl.lawinegevaar.exttablegen.xmlconfig.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static nl.lawinegevaar.exttablegen.TableDerivationConfig.DEFAULT_COLUMN_ENCODING;
import static nl.lawinegevaar.exttablegen.TableDerivationConfig.DEFAULT_END_COLUMN_TYPE;

/**
 * Maps between the internal model and XML model of the configuration file.
 */
final class ConfigMapper {

    static final String SCHEMA_VERSION_1_0 = "1.0";
    // Schema version for documents created by ext-table-gen v1.0
    static final String SCHEMA_VERSION_2_0 = "2.0";
    private static final Set<String> SUPPORTED_SCHEMA_VERSIONS = Set.of(SCHEMA_VERSION_1_0, SCHEMA_VERSION_2_0);

    // Must match /xs:schema[@version]
    static final String CURRENT_SCHEMA_VERSION = SCHEMA_VERSION_2_0;
    static final String UNKNOWN_SCHEMA_VERSION = SCHEMA_VERSION_1_0;

    private final ObjectFactory factory = new ObjectFactory();
    private final JAXBContext jaxbContext;
    {
        try {
            jaxbContext = JAXBContext.newInstance(ExtTableGenConfig.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Writes {@code etgConfig} as XML to {@code out}.
     *
     * @param etgConfig
     *         external-table-gen configuration
     * @param out
     *         output stream
     * @throws JAXBException
     *         for errors writing the XML
     */
    void write(EtgConfig etgConfig, OutputStream out) throws JAXBException {
        ExtTableGenConfig xmlConfigType = toXmlExtTableGenConfig(etgConfig);
        Marshaller marshaller = getMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(xmlConfigType, out);
    }

    private Marshaller getMarshaller() throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setListener(new ApplyDefaultsOnMarshallListener());
        return marshaller;
    }

    /**
     * Reads the external-table-gen configuration from {@code in}.
     *
     * @param in
     *         input stream
     * @return external-table-gen configuration
     * @throws JAXBException
     *         for errors reading or parsing the XML
     */
    EtgConfig read(InputStream in) throws JAXBException {
        ExtTableGenConfig extTableGenConfig = readAsExtTableGenConfig(in);
        return fromXmlExtTableGenConfig(extTableGenConfig);
    }

    /**
     * Reads the external-table-gen configuration from {@code in} as an instance of {@link ExtTableGenConfig}.
     *
     * @param in
     *         input stream
     * @return XML configuration object
     * @throws JAXBException
     *         for errors reading or parsing the XML
     */
    // package-private access for tests
    ExtTableGenConfig readAsExtTableGenConfig(InputStream in) throws JAXBException {
        Unmarshaller unmarshaller = getUnmarshaller();
        return (ExtTableGenConfig) unmarshaller.unmarshal(in);
    }

    private Unmarshaller getUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setListener(new ApplyDefaultsOnUnmarshallListener());
        return unmarshaller;
    }

    private ExtTableGenConfig toXmlExtTableGenConfig(EtgConfig etgConfig) {
        ExtTableGenConfig configType = factory.createExtTableGenConfig();
        TableConfig tableConfig = etgConfig.tableConfig();
        configType.setExternalTable(toXmlExternalTable(tableConfig));
        configType.setTableDerivation(toXmlTableDerivationType(etgConfig.tableDerivationConfig()));
        tableConfig.toDdl()
                .map(ddl -> {
                    InformationalType informationalType = factory.createInformationalType();
                    informationalType.setDdl(ddl);
                    return informationalType;
                })
                .ifPresent(configType::setInformational);
        etgConfig.csvFileConfig()
                .map(this::toXmlCsvFileType)
                .ifPresent(configType::setCsvFile);
        return configType;
    }

    private ExternalTableType toXmlExternalTable(TableConfig tableConfig) {
        ExternalTableType externalTableType = factory.createExternalTableType();
        externalTableType.setName(tableConfig.name());
        externalTableType.setColumns(toXmlColumnListType(tableConfig.columns()));
        externalTableType.setByteOrder(tableConfig.byteOrder().name());
        tableConfig.tableFile()
                .map(this::toXmlTableFileType)
                .ifPresent(externalTableType::setTableFile);
        return externalTableType;
    }

    private ColumnListType toXmlColumnListType(List<Column> columnList) {
        ColumnListType columnListType = factory.createColumnListType();
        if (columnList == null || columnList.isEmpty()) return columnListType;

        int size = columnList.size();
        if (columnList.get(size - 1) instanceof EndColumn endColumn) {
            columnList = columnList.subList(0, size - 1);
            columnListType.setEndColumn(toXmlEndColumnType(endColumn));
        }
        List<ColumnType> columnTypes = columnListType.getColumns();
        columnList.stream().map(this::toXmlColumnType).forEach(columnTypes::add);
        return columnListType;
    }

    private ColumnType toXmlColumnType(Column column) {
        if (column instanceof EndColumn) {
            throw new IllegalArgumentException("This method should not be called with an EndColumn, use toXmlEndColumnType");
        }
        ColumnType columnType = factory.createColumnType();
        columnType.setName(column.name());
        columnType.setDatatype(toXmlDataTypeElement(column.datatype()));
        return columnType;
    }

    private JAXBElement<? extends DatatypeType> toXmlDataTypeElement(FbDatatype<?> datatype) {
        JAXBElement<? extends DatatypeType> datatypeTypeElement = createXmlDataTypeElement(datatype);
        datatype.converter()
                .ifPresent(converter -> datatypeTypeElement.getValue().setConverter(toXmlConverterType(converter)));
        return datatypeTypeElement;
    }

    private JAXBElement<? extends DatatypeType> createXmlDataTypeElement(FbDatatype<?> datatype) {
        return switch (datatype.getClass().getSimpleName()) {
            case "FbChar" -> {
                FbChar fbChar = (FbChar) datatype;
                CharType charType = factory.createCharType();
                charType.setLength(fbChar.length());
                charType.setEncoding(fbChar.encoding().firebirdName());
                yield factory.createChar(charType);
            }
            case "FbInteger" -> factory.createInteger(factory.createDatatypeType());
            case "FbBigint" -> factory.createBigint(factory.createDatatypeType());
            case "FbSmallint" -> factory.createSmallint(factory.createDatatypeType());
            case "FbInt128" -> factory.createInt128(factory.createDatatypeType());
            case "FbDate" -> factory.createDate(factory.createDatatypeType());
            case "FbTime" -> factory.createTime(factory.createDatatypeType());
            case "FbTimestamp" -> factory.createTimestamp(factory.createDatatypeType());
            case "FbNumeric", "FbDecimal" -> {
                FbFixedPointDatatype fbFixedPoint = (FbFixedPointDatatype) datatype;
                FixedPointType fixedPointType = factory.createFixedPointType();
                fixedPointType.setPrecision(fbFixedPoint.precision());
                fixedPointType.setScale(fbFixedPoint.scale());
                fixedPointType.setRoundingMode(fbFixedPoint.roundingMode().name());
                yield switch (datatype.getClass().getSimpleName()) {
                    case "FbNumeric" -> factory.createNumeric(fixedPointType);
                    case "FbDecimal" -> factory.createDecimal(fixedPointType);
                    default -> throw new IllegalArgumentException(
                            "Unsupported Datatype class: " + datatype.getClass().getName());
                };
            }
            default ->
                    throw new IllegalArgumentException("Unsupported Datatype class: " + datatype.getClass().getName());
        };
    }

    private ConverterType toXmlConverterType(Converter<?> converter) {
        String converterName = converter.converterName();
        try {
            JAXBElement<? extends ConverterStepType> stepType = switch (converterName) {
                case "parseIntegralNumber" -> {
                    ParseIntegralType parseIntegralType = factory.createParseIntegralType();
                    parseIntegralType.setRadix(((AbstractParseIntegralNumber<?>) converter).radix());
                    yield factory.createParseIntegralNumber(parseIntegralType);
                }
                case "parseDatetime" -> {
                    ParseDatetimeType parseDatetimeType = factory.createParseDatetimeType();
                    ParseDatetime parseDatetime = (ParseDatetime) converter;
                    parseDatetimeType.setPattern(parseDatetime.pattern());
                    parseDatetime.locale().ifPresent(locale -> parseDatetimeType.setLocale(locale.toLanguageTag()));
                    yield factory.createParseDatetime(parseDatetimeType);
                }
                case "parseBigDecimal" -> {
                    ParseBigDecimalType parseBigDecimalType = factory.createParseBigDecimalType();
                    ParseBigDecimal parseBigDecimal = (ParseBigDecimal) converter;
                    parseBigDecimalType.setLocale(parseBigDecimal.locale().toLanguageTag());
                    yield factory.createParseBigDecimal(parseBigDecimalType);
                }
                default -> throw new InvalidConfigurationException("Unsupported converter: " + converterName);
            };
            ConverterType converterType = factory.createConverterType();
            converterType.setConverterStep(stepType);
            return converterType;
        } catch (InvalidConfigurationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Could not create XML ConverterType for " + converterName, e);
        }
    }

    private EndColumnType toXmlEndColumnType(EndColumn endColumn) {
        EndColumnType endColumnType = factory.createEndColumnType();
        endColumnType.setType(endColumn.name());
        return endColumnType;
    }

    private TableFileType toXmlTableFileType(TableFile tableFile) {
        TableFileType tableFileType = factory.createTableFileType();
        tableFileType.setPath(tableFile.path().toString());
        tableFileType.setOverwrite(tableFile.overwrite());
        return tableFileType;
    }

    private TableDerivationType toXmlTableDerivationType(TableDerivationConfig tableDerivationConfig) {
        TableDerivationType tableDerivationType = factory.createTableDerivationType();
        tableDerivationType.setColumnEncoding(tableDerivationConfig.columnEncoding().firebirdName());
        tableDerivationType.setEndColumnType(tableDerivationConfig.endColumnType().name());
        return tableDerivationType;
    }

    private CsvFileType toXmlCsvFileType(CsvFileConfig csvFileConfig) {
        CsvFileType csvFileType = factory.createCsvFileType();
        csvFileType.setPath(csvFileConfig.path().toString());
        csvFileType.setCharset(csvFileConfig.charset().name());
        csvFileType.setHeaderRow(csvFileConfig.headerRow());
        csvFileType.setCsvParser(toXmlCsvParserType(csvFileConfig.parserConfig()));
        return csvFileType;
    }

    private JAXBElement<? extends CsvParserType> toXmlCsvParserType(CsvParserConfig csvParserConfig) {
        return switch (csvParserConfig.type()) {
            case RFC_4180 -> toXmlRfc4180CsvParserType(csvParserConfig);
            case CUSTOM -> toXmlCustomCsvParserType(csvParserConfig);
        };
    }

    private JAXBElement<Rfc4180CsvParserType> toXmlRfc4180CsvParserType(CsvParserConfig csvParserConfig) {
        Rfc4180CsvParserType parserType = factory.createRfc4180CsvParserType();
        parserType.setQuoteChar(charValueAsStringOrNull(csvParserConfig.quoteChar()));
        parserType.setSeparator(charValueAsStringOrNull(csvParserConfig.separator()));
        return factory.createRfc4180CsvParser(parserType);
    }

    private static String charValueAsStringOrNull(CharValue charValue) {
        return charValue != null ? charValue.toString() : null;
    }

    private JAXBElement<CustomCsvParserType> toXmlCustomCsvParserType(CsvParserConfig csvParserConfig) {
        CustomCsvParserType parserType = factory.createCustomCsvParserType();
        parserType.setQuoteChar(charValueAsStringOrNull(csvParserConfig.quoteChar()));
        parserType.setSeparator(charValueAsStringOrNull(csvParserConfig.separator()));
        parserType.setEscapeChar(charValueAsStringOrNull(csvParserConfig.escapeChar()));
        parserType.setIgnoreLeadingWhiteSpace(csvParserConfig.ignoreLeadingWhiteSpace());
        parserType.setIgnoreQuotations(csvParserConfig.ignoreQuotations());
        parserType.setStrictQuotes(csvParserConfig.strictQuotes());
        return factory.createCustomCsvParser(parserType);
    }

    EtgConfig fromXmlExtTableGenConfig(ExtTableGenConfig extTableGenConfig) {
        if (!SUPPORTED_SCHEMA_VERSIONS.contains(extTableGenConfig.getSchemaVersion())) {
            throw new InvalidConfigurationException("Unsupported schema version %s, expected one of %s"
                    .formatted(extTableGenConfig.getSchemaVersion(), SUPPORTED_SCHEMA_VERSIONS));
        }
        try {
            return new EtgConfig(
                    fromXmlExternalTableType(extTableGenConfig.getExternalTable()),
                    fromXmlTableDerivationType(extTableGenConfig.getTableDerivation()),
                    fromXmlCsvFileType(extTableGenConfig.getCsvFile()));
        } catch (InvalidConfigurationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException(
                    "Cannot parse or convert ext-table-gen configuration; check if configuration file matches XSD and "
                    + "if its values are correct", e);
        }
    }

    private static TableConfig fromXmlExternalTableType(ExternalTableType externalTableType) {
        if (externalTableType == null) {
            return TableConfig.empty();
        }
        return new TableConfig(
                externalTableType.getName(),
                fromXmlColumnListType(externalTableType.getColumns()),
                fromXmlTableFileType(externalTableType.getTableFile()),
                fromXmlByteOrderEnum(externalTableType.getByteOrder()));
    }

    private static List<Column> fromXmlColumnListType(ColumnListType columnListType) {
        if (columnListType == null) {
            return List.of();
        }
        try {
            return Stream.concat(
                            columnListType.getColumns().stream().map(ConfigMapper::fromXmlColumnType),
                            Optional.ofNullable(columnListType.getEndColumn()).map(ConfigMapper::fromXmlEndColumnType)
                                    .stream())
                    .toList();
        } catch (InvalidConfigurationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Could not convert from XML ColumnListType", e);
        }
    }

    private static Column fromXmlColumnType(ColumnType columnType) {
        JAXBElement<? extends DatatypeType> datatype = columnType.getDatatype();
        return new Column(columnType.getName(),
                fromXmlDatatype(datatype).withConverterChecked(fromXmlConverter(datatype)));
    }

    private static FbDatatype<?> fromXmlDatatype(JAXBElement<? extends DatatypeType> datatype) {
        return switch (datatype.getName().getLocalPart()) {
            case "char" -> {
                CharType charType = (CharType) datatype.getValue();
                yield new FbChar(charType.getLength(), FbEncoding.forName(charType.getEncoding()));
            }
            case "integer" -> new FbInteger();
            case "bigint" -> new FbBigint();
            case "smallint" -> new FbSmallint();
            case "int128" -> new FbInt128();
            case "date" -> new FbDate();
            case "time" -> new FbTime();
            case "timestamp" -> new FbTimestamp();
            case "numeric" -> {
                FixedPointType fixedPointType = (FixedPointType) datatype.getValue();
                yield new FbNumeric(fixedPointType.getPrecision(), fixedPointType.getScale(),
                        RoundingMode.valueOf(fixedPointType.getRoundingMode()));
            }
            case "decimal" -> {
                FixedPointType fixedPointType = (FixedPointType) datatype.getValue();
                yield new FbDecimal(fixedPointType.getPrecision(), fixedPointType.getScale(),
                        RoundingMode.valueOf(fixedPointType.getRoundingMode()));
            }
            default -> throw new InvalidConfigurationException(
                    "Unsupported DatatypeType: " + datatype.getDeclaredType().getName());
        };
    }

    private static Converter<?> fromXmlConverter(JAXBElement<? extends DatatypeType> datatype) {
        ConverterType converterType = datatype.getValue().getConverter();
        if (converterType == null) return null;
        JAXBElement<? extends ConverterStepType> converterStepElement = converterType.getConverterStep();
        ConverterStepType converterStep = converterStepElement.getValue();
        if (converterStep instanceof ParseIntegralType parseIntegralType) {
            return fromXmlParseIntegralType(parseIntegralType, datatype);
        } else if (converterStep instanceof ParseDatetimeType parseDatetimeType) {
            return Converter.parseDatetime(parseDatetimeType.getPattern(), parseDatetimeType.getLocale());
        } else if (converterStep instanceof ParseBigDecimalType parseBigDecimalType) {
            return Converter.parseBigDecimal(parseBigDecimalType.getLocale());
        } else {
            throw new InvalidConfigurationException("Unsupported element: " + converterStepElement.getName());
        }
    }

    private static Converter<?> fromXmlParseIntegralType(ParseIntegralType parseIntegralType,
            JAXBElement<? extends DatatypeType> datatype) {
        try {
            return Converter.parseIntegralNumber(datatype.getName().getLocalPart(), parseIntegralType.getRadix());
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException(
                    "Unsupported ParseIntegralType data type: " + datatype.getName(), e);
        }
    }

    private static EndColumn fromXmlEndColumnType(EndColumnType endColumnType) {
        try {
            return EndColumn.require(EndColumn.Type.valueOf(endColumnType.getType()));
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Unsupported end column type: " + endColumnType.getType(), e);
        }
    }

    private static Optional<TableFile> fromXmlTableFileType(TableFileType tableFileType) {
        if (tableFileType == null) return Optional.empty();
        try {
            return Optional.of(
                    new TableFile(
                            tableFileType.getPath(),
                            tableFileType.isOverwrite()));
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Could not convert from XML TableFileType", e);
        }
    }

    private static ByteOrderType fromXmlByteOrderEnum(String byteOrderEnumValue) {
        if (byteOrderEnumValue == null) {
            return ByteOrderType.AUTO;
        }
        try {
            return ByteOrderType.valueOf(byteOrderEnumValue);
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Unsupported byte order type: " + byteOrderEnumValue);
        }
    }

    private static TableDerivationConfig fromXmlTableDerivationType(TableDerivationType tableDerivationType) {
        return new TableDerivationConfig(
                fromXmlColumnEncoding(tableDerivationType),
                fromXmlEndColumnTypeName(tableDerivationType),
                TableDerivationMode.NEVER);
    }

    private static FbEncoding fromXmlColumnEncoding(TableDerivationType tableDerivationType) {
        String columnEncoding = tableDerivationType != null ? tableDerivationType.getColumnEncoding() : null;
        if (columnEncoding == null) {
            return DEFAULT_COLUMN_ENCODING;
        }
        try {
            return FbEncoding.forName(columnEncoding);
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException(
                    "Invalid value for tableDerivation[@columnEncoding]: " + columnEncoding, e);
        }
    }

    private static EndColumn.Type fromXmlEndColumnTypeName(TableDerivationType tableDerivationType) {
        String endColumnType = tableDerivationType != null ? tableDerivationType.getEndColumnType() : null;
        if (endColumnType == null) {
            return DEFAULT_END_COLUMN_TYPE;
        }
        try {
            return EndColumn.Type.valueOf(endColumnType);
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException(
                    "Could not convert from tableDerivation[@endColumnType]: " + endColumnType, e);
        }
    }

    private static Optional<CsvFileConfig> fromXmlCsvFileType(CsvFileType csvFileType) {
        if (csvFileType == null) return Optional.empty();
        try {
            return Optional.of(
                    new CsvFileConfig(
                            csvFileType.getPath(),
                            csvFileType.getCharset(),
                            csvFileType.isHeaderRow(),
                            fromXmlCsvParserConfig(csvFileType.getCsvParser())));
        } catch (InvalidConfigurationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Could not convert from XML CsvFileType", e);
        }
    }

    private static CsvParserConfig fromXmlCsvParserConfig(JAXBElement<? extends CsvParserType> csvParserElement) {
        return fromXmlCsvParserConfig(csvParserElement != null ? csvParserElement.getValue() : null);
    }

    private static CsvParserConfig fromXmlCsvParserConfig(CsvParserType csvParserType) {
        if (csvParserType == null) {
            return CsvParserConfig.of();
        } else if (csvParserType instanceof Rfc4180CsvParserType rfc4180CsvParserType) {
            return fromXmlRfc4180CsvParserType(rfc4180CsvParserType);
        } else if (csvParserType instanceof CustomCsvParserType customCsvParserType) {
            return fromXmlCustomCsvParserType(customCsvParserType);
        } else {
            throw new InvalidConfigurationException("Unsupported CsvParserType: " + csvParserType.getClass().getName());
        }
    }

    private static CsvParserConfig fromXmlRfc4180CsvParserType(Rfc4180CsvParserType rfc4180CsvParserType) {
        try {
            return CsvParserConfig.rfc4180(
                    CharValue.of(rfc4180CsvParserType.getQuoteChar()),
                    CharValue.of(rfc4180CsvParserType.getSeparator()));
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Could not convert from XML Rfc4180CsvParserType", e);
        }
    }

    private static CsvParserConfig fromXmlCustomCsvParserType(CustomCsvParserType customCsvParserType) {
        try {
            return CsvParserConfig.custom(
                    CharValue.of(customCsvParserType.getQuoteChar()),
                    CharValue.of(customCsvParserType.getSeparator()),
                    CharValue.of(customCsvParserType.getEscapeChar()),
                    customCsvParserType.isIgnoreLeadingWhiteSpace(),
                    customCsvParserType.isIgnoreQuotations(),
                    customCsvParserType.isStrictQuotes());
        } catch (RuntimeException e) {
            throw new InvalidConfigurationException("Could not convert from XML CustomCsvParserType", e);
        }
    }

    private static final class ApplyDefaultsOnMarshallListener extends Marshaller.Listener {

        @Override
        public void beforeMarshal(Object source) {
            if (source instanceof ExtTableGenConfig extTableGenConfig) {
                // Set or update schema version
                extTableGenConfig.setSchemaVersion(CURRENT_SCHEMA_VERSION);
            }
        }

    }

    private static final class ApplyDefaultsOnUnmarshallListener extends Unmarshaller.Listener {

        @Override
        public void afterUnmarshal(Object target, Object parent) {
            if (target instanceof ExtTableGenConfig extTableGenConfig && extTableGenConfig.getSchemaVersion() == null) {
                extTableGenConfig.setSchemaVersion(UNKNOWN_SCHEMA_VERSION);
            }
        }

    }

}
