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

/**
 * Maps between the internal model and XML model of the configuration file.
 */
class ConfigMapper {

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
        tableConfig.outputConfig()
                .map(this::toXmlOutputConfigType)
                .ifPresent(configType::setOutputConfig);
        configType.setTableDerivationConfig(toXmlTableDerivationConfigType(etgConfig.tableDerivationConfig()));
        tableConfig.toDdl()
                .map(ddl -> {
                    InformationalType informationalType = factory.createInformationalType();
                    informationalType.setDdl(ddl);
                    return informationalType;
                })
                .ifPresent(configType::setInformational);
        etgConfig.inputConfig()
                .map(this::toXmlInputConfigType)
                .ifPresent(configType::setInputConfig);
        return configType;
    }

    private ExternalTableType toXmlExternalTable(TableConfig tableConfig) {
        ExternalTableType externalTableType = factory.createExternalTableType();
        externalTableType.setName(tableConfig.name());
        externalTableType.setColumns(toXmlColumnListType(tableConfig.columns()));
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

    private OutputConfigType toXmlOutputConfigType(OutputConfig outputConfig) {
        OutputConfigType outputConfigType = factory.createOutputConfigType();
        outputConfigType.setPath(outputConfig.path().toString());
        outputConfigType.setAllowOverwrite(outputConfig.allowOverwrite());
        return outputConfigType;
    }

    private TableDerivationConfigType toXmlTableDerivationConfigType(TableDerivationConfig tableDerivationConfig) {
        TableDerivationConfigType tableDerivationConfigType = factory.createTableDerivationConfigType();
        tableDerivationConfigType.setColumnEncoding(tableDerivationConfig.columnEncoding().firebirdName());
        tableDerivationConfigType.setEndColumnType(tableDerivationConfig.endColumnType().name());
        return tableDerivationConfigType;
    }

    private InputConfigType toXmlInputConfigType(InputConfig inputConfig) {
        InputConfigType inputConfigType = factory.createInputConfigType();
        inputConfigType.setPath(inputConfig.path().toString());
        inputConfigType.setCharset(inputConfig.charset().name());
        inputConfigType.setHasHeaderRow(inputConfig.hasHeaderRow());
        return inputConfigType;
    }

    private EtgConfig fromXmlExtTableGenConfig(ExtTableGenConfig extTableGenConfig) {
        return new EtgConfig(
                fromXmlExternalTableType(extTableGenConfig.getExternalTable())
                        .withOutputConfig(fromXmlOutputConfigType(extTableGenConfig.getOutputConfig())),
                fromXmlTableDerivationConfigType(extTableGenConfig.getTableDerivationConfig()),
                fromXmlInputConfigType(extTableGenConfig.getInputConfig()));
    }

    private TableConfig fromXmlExternalTableType(ExternalTableType externalTableType) {
        return new TableConfig(
                externalTableType.getName(),
                fromXmlColumnListType(externalTableType.getColumns()),
                Optional.empty());
    }

    private List<Column> fromXmlColumnListType(ColumnListType columnListType) {
        return Stream.concat(
                        columnListType.getColumns().stream().map(this::fromXmlColumnType),
                        Optional.ofNullable(columnListType.getEndColumn()).map(this::fromXmlEndColumnType).stream())
                .toList();
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

    private Optional<OutputConfig> fromXmlOutputConfigType(OutputConfigType outputConfigType) {
        if (outputConfigType == null) return Optional.empty();
        try {
            return Optional.of(
                    new OutputConfig(
                            outputConfigType.getPath(),
                            outputConfigType.isAllowOverwrite()));
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(WARNING, "Could not convert from XML OutputConfigType, output config value dropped", e);
            return Optional.empty();
        }
    }

    private TableDerivationConfig fromXmlTableDerivationConfigType(TableDerivationConfigType tableDerivationConfigType) {
        String encodingName = tableDerivationConfigType.getColumnEncoding();
        String endColumnTypeName = tableDerivationConfigType.getEndColumnType();
        return new TableDerivationConfig(
                encodingName != null ? FbEncoding.forName(encodingName) : FbEncoding.ISO8859_1,
                endColumnTypeName != null ? EndColumn.Type.valueOf(endColumnTypeName) : EndColumn.Type.LF,
                TableDerivationMode.NEVER);
    }

    private Optional<InputConfig> fromXmlInputConfigType(InputConfigType inputConfigType) {
        if (inputConfigType == null) return Optional.empty();
        try {
            return Optional.of(
                    new InputConfig(
                            inputConfigType.getPath(),
                            inputConfigType.getCharset(),
                            inputConfigType.isHasHeaderRow()));
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(WARNING, "Could not convert from XML InputConfigType, input config value dropped", e);
            return Optional.empty();
        }
    }

}
