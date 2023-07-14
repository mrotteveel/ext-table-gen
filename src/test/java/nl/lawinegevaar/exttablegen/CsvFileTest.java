// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowFunctionValidator;
import nl.lawinegevaar.exttablegen.type.FbEncoding;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("HttpUrlsUsage")
class CsvFileTest {

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void readFile_happyPath(boolean readHeader) {
        var inputResource = InputResource.fromClasspath("/testdata/customers-10.csv");

        var csvFile = new CsvFile(inputResource, new CsvFile.Config(ISO_8859_1, readHeader ? 0 : 1, readHeader));
        var consumer = new TestConsumer(Set.of(1, 10));
        ProcessingResult result = csvFile.readFile(consumer);

        assertInstanceOf(ProcessingResult.Done.class, result, "expected Done signal");
        assertTrue(consumer.receivedOnComplete, "onComplete not received");
        assertEquals(10, consumer.currentRow, "unexpected number of rows read");
        assertTrue(consumer.receivedExceptions.isEmpty(),
                () -> "unexpected exception received: " + consumer.receivedExceptions);
        if (readHeader) {
            assertEquals(
                    new Row(1, List.of("Index", "Customer Id", "First Name", "Last Name", "Company", "City",
                            "Country", "Phone 1", "Phone 2", "Email", "Subscription Date", "Website")),
                    consumer.header, "unexpected header");
        } else {
            assertEquals(Row.noHeader(), consumer.header, "expected no header");
        }
        assertEquals(2, consumer.sampleRows.size(), "expected two sample rows");
        assertEquals(
                new Row(2, List.of("1", "DD37Cf93aecA6Dc", "Sheryl", "Baxter", "Rasmussen Group", "East Leonard",
                        "Chile", "229.077.5154", "397.884.0519x718", "zunigavanessa@smith.info", "2020-08-24",
                        "http://www.stephenson.com/")),
                consumer.sampledRows.get(0), "unexpected row 1");
        assertEquals(
                new Row(11, List.of("10", "8C2811a503C7c5a", "Michelle", "Gallagher", "Beck-Hendrix", "Elaineberg",
                        "Timor-Leste", "739.218.2516x459", "001-054-401-0347x617", "mdyer@escobar.net", "2021-11-08",
                        "https://arias.com/")),
                consumer.sampledRows.get(1), "unexpected row 10");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void readFile_empty(boolean readHeader) {
        var csvFile = new CsvFile(InputResource.of(new byte[0]), new CsvFile.Config(ISO_8859_1, 0, readHeader));
        var consumer = new TestConsumer(Set.of(1));
        ProcessingResult result = csvFile.readFile(consumer);

        assertInstanceOf(ProcessingResult.Done.class, result, "expected Done signal");
        assertTrue(consumer.receivedOnComplete, "onComplete not received");
        assertEquals(0, consumer.currentRow, "unexpected number of rows read");
        assertTrue(consumer.receivedExceptions.isEmpty(),
                () -> "unexpected exception received: " + consumer.receivedExceptions);
        // Difference is: if a file is empty, when we expect to read a header, onHeader(...) is never called,
        // but when no header is to be read, then onHeader(Row.noHeader()) is called
        assertEquals(readHeader ? TestConsumer.NOT_SET : Row.noHeader(), consumer.header, "expected no header");
        assertTrue(consumer.sampledRows.isEmpty(), "expected no sampled rows");
    }

    @Test
    void readFile_onlyHeader() {
        var inputResource = InputResource.of("""
                column1,column2
                """,
                ISO_8859_1);

        var csvFile = new CsvFile(inputResource, new CsvFile.Config(ISO_8859_1, 0, true));
        var consumer = new TestConsumer(Set.of(1));
        ProcessingResult result = csvFile.readFile(consumer);

        assertInstanceOf(ProcessingResult.Done.class, result, "expected Done signal");
        assertTrue(consumer.receivedOnComplete, "onComplete not received");
        assertEquals(0, consumer.currentRow, "unexpected number of rows read");
        assertTrue(consumer.receivedExceptions.isEmpty(),
                () -> "unexpected exception received: " + consumer.receivedExceptions);
        assertEquals(new Row(1, List.of("column1", "column2")), consumer.header, "unexpected header");
        assertTrue(consumer.sampledRows.isEmpty(), "expected no sampled rows");
    }

    @Test
    void readFile_continueOnException() {
        var inputResource = InputResource.of("""
                column1,column2
                triggerError,row1value2
                row2value1,row2value2
                """,
                ISO_8859_1);

        var csvFile = new CsvFile(inputResource,
                new CsvFile.Config(ISO_8859_1, 0, true, addValidatorForErrorOnWord("triggerError")));
        var consumer = new TestConsumer(Set.of(1, 2));
        ProcessingResult result = csvFile.readFile(consumer);

        assertInstanceOf(ProcessingResult.Done.class, result, "expected Done signal");
        assertTrue(consumer.receivedOnComplete, "onComplete not received");
        // The exception also "consumes" a row
        assertEquals(2, consumer.currentRow, "unexpected number of rows read");
        assertEquals(1, consumer.receivedExceptions.size(), "unexpected exceptions");
        TestConsumer.ExceptionOnRow exceptionOnRow = consumer.receivedExceptions.get(0);
        assertEquals(1, exceptionOnRow.row(), "unexpected row with exception");
        assertInstanceOf(CsvValidationException.class, exceptionOnRow.exception, "unexpected exception");
        assertEquals(new Row(1, List.of("column1", "column2")), consumer.header, "unexpected header");
        assertEquals(1, consumer.sampledRows.size(), "expected one sample row");
        assertEquals(new Row(3, List.of("row2value1", "row2value2")), consumer.sampledRows.get(0), "unexpected row 2");
    }

    @Test
    void readFile_incorrectlyQuotedRow() {
        var inputResource = InputResource.of("""
                column1,column2
                "no end quote,row1value2
                row2value1,row2value2
                """,
                ISO_8859_1);
        var csvFile = new CsvFile(inputResource,
                new CsvFile.Config(ISO_8859_1, 0, true, addValidatorForErrorOnWord("triggerError")));
        var consumer = new TestConsumer(Set.of(1, 2));
        assertThrows(FatalRowProcessingException.class, () -> csvFile.readFile(consumer));
        assertTrue(consumer.receivedOnComplete, "onComplete not received");
        // The exception "consumes" a row
        assertEquals(1, consumer.currentRow, "unexpected number of rows read");
        assertEquals(1, consumer.receivedExceptions.size(), "unexpected exceptions");
        TestConsumer.ExceptionOnRow exceptionOnRow = consumer.receivedExceptions.get(0);
        assertEquals(1, exceptionOnRow.row(), "unexpected row with exception");
        assertInstanceOf(FatalRowProcessingException.class, exceptionOnRow.exception, "unexpected exception");
        assertEquals(new Row(1, List.of("column1", "column2")), consumer.header, "unexpected header");
        assertTrue(consumer.sampledRows.isEmpty(), "expected no sampled rows");
    }

    @Test
    void readFile_incorrectlyQuotedHeader() {
        var inputResource = InputResource.of("""
                "column1,column2
                row1value1,row1value2
                row2value1,row2value2
                """,
                ISO_8859_1);
        var csvFile = new CsvFile(inputResource,
                new CsvFile.Config(ISO_8859_1, 0, true, addValidatorForErrorOnWord("triggerError")));
        var consumer = new TestConsumer(Set.of(1, 2));
        assertThrows(FatalRowProcessingException.class, () -> csvFile.readFile(consumer));
        assertTrue(consumer.receivedOnComplete, "onComplete not received");
        // The exception "consumes" a row
        assertEquals(1, consumer.currentRow, "unexpected number of rows read");
        assertEquals(1, consumer.receivedExceptions.size(), "unexpected exceptions");
        TestConsumer.ExceptionOnRow exceptionOnRow = consumer.receivedExceptions.get(0);
        assertEquals(1, exceptionOnRow.row(), "unexpected row with exception");
        assertInstanceOf(FatalRowProcessingException.class, exceptionOnRow.exception, "unexpected exception");
        assertSame(TestConsumer.NOT_SET, consumer.header, "unexpected header");
        assertTrue(consumer.sampledRows.isEmpty(), "expected no sampled rows");
    }

    /**
     * Not really a test, but intended to generate a test file to check on a real Firebird system.
     */
    @Disabled
    @Test
    void createTestData() throws Exception {
        // NOTE: The following paths are specific to one of my systems (mrotteveel)
        var inputResource = InputResource.fromClasspath("D:/Development/data/testdata-csv/customers-100.csv");
        Path outputFile = Path.of("E:/DB/exttables/test-customers.dat");
        var csvFile = new CsvFile(inputResource, new CsvFile.Config(ISO_8859_1, 0, true));
        var externalTable = ExternalTable.deriveFrom(csvFile,
                new ExternalTable.Config("TEST_CUSTOMERS", outputFile, FbEncoding.forName("WIN1252"),
                        EndColumn.Type.LF, ByteOrderType.AUTO));
        System.out.println(externalTable.toCreateTableStatement());

        try (var writer = new ExternalTableWriter(externalTable, OutputResource.of(outputFile, true))) {
            ProcessingResult result = csvFile.readFile(writer);
            if (result instanceof ProcessingResult.StopWithException swe) {
                throw swe.exception();
            }
            writer.getLastException().ifPresent(Throwable::printStackTrace);
        }
    }

    private Consumer<CSVReaderBuilder> addValidatorForErrorOnWord(String word) {
        return b -> b.withRowValidator(
                new RowFunctionValidator(
                        row -> {
                            for (String columnValue : row) {
                                if (Objects.equals(columnValue, word)) {
                                    return false;
                                }
                            }
                            return true;
                        },
                        "row contains column with value '%s'".formatted(word)));
    }

    /**
     * Test implementation of {@link RowProcessor}.
     * <p>
     * Should not be reused, create a new instance when needed.
     * </p>
     */
    private static final class TestConsumer implements RowProcessor {

        static final Row NOT_SET = new Row(0, List.of());

        private final Set<Integer> sampleRows;
        private int currentRow;
        private boolean receivedOnComplete;

        Row header = NOT_SET;
        final List<Row> sampledRows = new ArrayList<>();
        final List<ExceptionOnRow> receivedExceptions = new ArrayList<>();

        private TestConsumer(Set<Integer> sampleRows) {
            this.sampleRows = sampleRows;
        }

        @Override
        public ProcessingResult onHeader(Row header) {
            this.header = header;
            return ProcessingResult.continueProcessing();
        }

        @Override
        public ProcessingResult onRow(Row row) {
            if (sampleRows.contains(++currentRow)) {
                sampledRows.add(row);
            }
            return ProcessingResult.continueProcessing();
        }

        @Override
        public ProcessingResult onException(Exception exception) {
            receivedExceptions.add(new ExceptionOnRow(++currentRow, exception));
            return ProcessingResult.continueProcessing();
        }

        @Override
        public ProcessingResult.Stop onComplete() {
            if (receivedOnComplete) {
                throw new IllegalStateException("Received multiple onComplete signals");
            }
            receivedOnComplete = true;
            return ProcessingResult.stopProcessing();
        }

        record ExceptionOnRow(int row, Exception exception) {
        }
    }
}