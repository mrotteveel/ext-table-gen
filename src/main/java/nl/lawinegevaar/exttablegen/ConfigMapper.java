// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import nl.lawinegevaar.exttablegen.xmlconfig.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.System.Logger.Level.WARNING;
import static nl.lawinegevaar.exttablegen.TableDerivationConfig.DEFAULT_COLUMN_ENCODING;
import static nl.lawinegevaar.exttablegen.TableDerivationConfig.DEFAULT_END_COLUMN_TYPE;

/**
 * Maps between the internal model and XML model of the configuration file.
 */
final class ConfigMapper {

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
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(xmlConfigType, out);
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
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ExtTableGenConfig extTableGenConfig = (ExtTableGenConfig) unmarshaller.unmarshal(in);
        return fromXmlExtTableGenConfig(extTableGenConfig);
    }

    private ExtTableGenConfig toXmlExtTableGenConfig(EtgConfig etgConfig) {
        ExtTableGenConfig configType = factory.createExtTableGenConfig();
        TableConfig tableConfig = etgConfig.tableConfig();
        configType.setExternalTable(toXmlExternalTable(tableConfig));
        configType.setTableDerivationConfig(toXmlTableDerivationConfigType(etgConfig.tableDerivationConfig()));
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
        columnType.setDatatype(toDataTypeElement(column.datatype()));
        return columnType;
    }

    private JAXBElement<? extends DatatypeType> toDataTypeElement(Datatype datatype) {
        if (toXmlDatatypeType(datatype) instanceof CharType charType) {
            return factory.createChar(charType);
        }
        throw new IllegalArgumentException("Unsupported Datatype class: " + datatype.getClass().getName());
    }

    private DatatypeType toXmlDatatypeType(Datatype datatype) {
        if (datatype instanceof Char charInstance) {
            CharType charType = factory.createCharType();
            charType.setLength(charInstance.length());
            charType.setEncoding(charInstance.encoding().firebirdName());
            return charType;
        }
        throw new IllegalArgumentException("Unsupported Datatype class: " + datatype.getClass().getName());
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

    private TableDerivationConfigType toXmlTableDerivationConfigType(TableDerivationConfig tableDerivationConfig) {
        TableDerivationConfigType tableDerivationConfigType = factory.createTableDerivationConfigType();
        tableDerivationConfigType.setColumnEncoding(tableDerivationConfig.columnEncoding().firebirdName());
        tableDerivationConfigType.setEndColumnType(tableDerivationConfig.endColumnType().name());
        return tableDerivationConfigType;
    }

    private CsvFileType toXmlCsvFileType(CsvFileConfig csvFileConfig) {
        CsvFileType csvFileType = factory.createCsvFileType();
        csvFileType.setPath(csvFileConfig.path().toString());
        csvFileType.setCharset(csvFileConfig.charset().name());
        csvFileType.setHeaderRow(csvFileConfig.headerRow());
        return csvFileType;
    }

    private EtgConfig fromXmlExtTableGenConfig(ExtTableGenConfig extTableGenConfig) {
        return new EtgConfig(
                fromXmlExternalTableType(extTableGenConfig.getExternalTable()),
                fromXmlTableDerivationConfigType(extTableGenConfig.getTableDerivationConfig()),
                fromXmlCsvFileType(extTableGenConfig.getCsvFile()));
    }

    private TableConfig fromXmlExternalTableType(ExternalTableType externalTableType) {
        if (externalTableType == null) {
            return TableConfig.empty();
        }
        return new TableConfig(
                externalTableType.getName(),
                fromXmlColumnListType(externalTableType.getColumns()),
                fromXmlTableFileType(externalTableType.getTableFile()));
    }

    private List<Column> fromXmlColumnListType(ColumnListType columnListType) {
        if (columnListType == null) {
            return List.of();
        }
        try {
            return Stream.concat(
                            columnListType.getColumns().stream().map(this::fromXmlColumnType),
                            Optional.ofNullable(columnListType.getEndColumn()).map(this::fromXmlEndColumnType).stream())
                    .toList();
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(WARNING, "Could not convert from XML ColumnListType, using empty column list", e);
            return List.of();
        }
    }

    private Column fromXmlColumnType(ColumnType columnType) {
        return new Column(columnType.getName(), fromXmlDatatype(columnType.getDatatype()));
    }

    private Datatype fromXmlDatatype(JAXBElement<? extends DatatypeType> datatype) {
        return fromXmlDatatypeType(datatype.getValue());
    }

    private Datatype fromXmlDatatypeType(DatatypeType datatypeType) {
        if (datatypeType instanceof CharType charType) {
            return new Char(charType.getLength(), FbEncoding.forName(charType.getEncoding()));
        }
        throw new IllegalArgumentException("Unsupported DatatypeType: " + datatypeType.getClass().getName());
    }

    private EndColumn fromXmlEndColumnType(EndColumnType endColumnType) {
        return EndColumn.require(EndColumn.Type.valueOf(endColumnType.getType()));
    }

    private Optional<TableFile> fromXmlTableFileType(TableFileType tableFileType) {
        if (tableFileType == null) return Optional.empty();
        try {
            return Optional.of(
                    new TableFile(
                            tableFileType.getPath(),
                            tableFileType.isOverwrite()));
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(WARNING, "Could not convert from XML TableFileType, table file value dropped", e);
            return Optional.empty();
        }
    }

    private TableDerivationConfig fromXmlTableDerivationConfigType(
            TableDerivationConfigType tableDerivationConfigType) {
        return new TableDerivationConfig(
                fromXmlColumnEncoding(tableDerivationConfigType),
                fromXmlEndColumnTypeName(tableDerivationConfigType),
                TableDerivationMode.NEVER);
    }

    /**
     * Converts {@link TableDerivationConfigType#getColumnEncoding()} to {@link FbEncoding}, falling back to
     * {@link TableDerivationConfig#DEFAULT_COLUMN_ENCODING} if invalid or if {@code tableDerivationConfigType} is {@code null}.
     *
     * @param tableDerivationConfigType
     *         XML table derivation config
     * @return end column type matching {@code endColumnTypeName}, or default encoding if it isn't a valid name or
     * {@code tableDerivationConfigType} is null
     */
    private FbEncoding fromXmlColumnEncoding(TableDerivationConfigType tableDerivationConfigType) {
        try {
            if (tableDerivationConfigType != null && tableDerivationConfigType.getColumnEncoding() != null) {
                return FbEncoding.forName(tableDerivationConfigType.getColumnEncoding());
            }
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(WARNING, "Could not convert from tableDerivationConfigType.getColumnEncoding()", e);
        }
        return DEFAULT_COLUMN_ENCODING;
    }

    /**
     * Converts {@link TableDerivationConfigType#getEndColumnType()} to {@link EndColumn.Type}, falling back to
     * {@link TableDerivationConfig#DEFAULT_END_COLUMN_TYPE} if invalid or if {@code tableDerivationConfigType} is
     * {@code null}.
     *
     * @param tableDerivationConfigType
     *         XML table derivation config
     * @return end column type matching {@code endColumnTypeName}, or default type if it isn't a valid name or
     * {@code tableDerivationConfigType} is null
     */
    private EndColumn.Type fromXmlEndColumnTypeName(TableDerivationConfigType tableDerivationConfigType) {
        try {
            if (tableDerivationConfigType != null && tableDerivationConfigType.getEndColumnType() != null) {
                return EndColumn.Type.valueOf(tableDerivationConfigType.getEndColumnType());
            }
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(WARNING, "Could not convert from tableDerivationConfigType.getEndColumnType()", e);
        }
        return DEFAULT_END_COLUMN_TYPE;
    }

    private Optional<CsvFileConfig> fromXmlCsvFileType(CsvFileType csvFileType) {
        if (csvFileType == null) return Optional.empty();
        try {
            return Optional.of(
                    new CsvFileConfig(
                            csvFileType.getPath(),
                            csvFileType.getCharset(),
                            csvFileType.isHeaderRow()));
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(WARNING, "Could not convert from XML CsvFileType, CSV file value dropped", e);
            return Optional.empty();
        }
    }

}
