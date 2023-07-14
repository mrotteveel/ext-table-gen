// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbEncoding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static nl.lawinegevaar.exttablegen.ColumnFixtures.col;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalTableWriterTest {

    @TempDir
    private Path tempDir;
    private Path externalFilePath;

    @BeforeEach
    void initPaths() {
        externalFilePath = tempDir.resolve("test-write.dat");
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @Test
    void writeTable_happyPath() throws Exception {
        try (ExternalTableWriter writer = createExternalTableWriter(externalFilePath, false)) {
            assertInstanceOf(ProcessingResult.Continue.class, writer.onHeader(Row.noHeader()));

            assertInstanceOf(ProcessingResult.Continue.class, writer.onRow(new Row(1, List.of("A", "ABCDE"))));
            assertInstanceOf(ProcessingResult.Continue.class, writer.onRow(new Row(2, List.of("", ""))));
            assertInstanceOf(ProcessingResult.Continue.class, writer.onRow(new Row(3, List.of("AB", "ABCDEF"))));
            assertInstanceOf(ProcessingResult.Continue.class, writer.onRow(new Row(4, List.of("A", "A"))));
            assertInstanceOf(ProcessingResult.Continue.class, writer.onRow(new Row(5, List.of("\u00e9", "\u00e9"))));

            assertInstanceOf(ProcessingResult.Stop.class, writer.onComplete());

            String fileContent = Files.readString(externalFilePath, Charset.forName("windows-1252"));

            assertEquals("""
                            AABCDE\r
                                  \r
                            AABCDE\r
                            AA    \r
                            ?\u00e9    \r
                            """,
                    fileContent);
        }
    }

    @Test
    void multipleCallsToOnHeader_throwsIllegalStateException() throws Exception {
        try (ExternalTableWriter writer = createExternalTableWriter(externalFilePath, true)) {
            assertInstanceOf(ProcessingResult.Continue.class, writer.onHeader(Row.noHeader()));

            assertThrows(IllegalStateException.class, () -> writer.onHeader(Row.noHeader()));
        }
    }

    @Test
    void onRowWithoutOnHeader_throwsIllegalStateException() throws Exception {
        try (ExternalTableWriter writer = createExternalTableWriter(externalFilePath, true)) {
            assertThrows(IllegalStateException.class, () -> writer.onRow(new Row(1, List.of("A", "B"))));
        }
    }

    @Test
    void allowOverwrite() throws Exception {
        Files.writeString(externalFilePath, "ORIGINAL CONTENT");
        try (ExternalTableWriter writer = createExternalTableWriter(externalFilePath, true)) {
            assertInstanceOf(ProcessingResult.Continue.class, writer.onHeader(Row.noHeader()));

            assertInstanceOf(ProcessingResult.Continue.class, writer.onRow(new Row(1, List.of("A", "ABCDE"))));

            assertInstanceOf(ProcessingResult.Stop.class, writer.onComplete());

            String fileContent = Files.readString(externalFilePath, StandardCharsets.US_ASCII);

            assertEquals("""
                    AABCDE\r
                    """,
                    fileContent);
        }
    }

    @Test
    void disallowOverwrite() throws Exception {
        Files.writeString(externalFilePath, "ORIGINAL CONTENT");
        try (ExternalTableWriter writer = createExternalTableWriter(externalFilePath, false)) {
            var swe = assertInstanceOf(ProcessingResult.StopWithException.class, writer.onHeader(Row.noHeader()));
            assertInstanceOf(InvalidTableException.class, swe.exception());
            assertInstanceOf(FileAlreadyExistsException.class, swe.exception().getCause());
        }
    }

    private static ExternalTableWriter createExternalTableWriter(Path externalFilePath, boolean allowOverWrite) {
        var outputResource = OutputResource.of(externalFilePath, allowOverWrite);
        var externalTable = new ExternalTable(
                "TEST_WRITE",
                List.of(col("COLUMN_1", 1, FbEncoding.ASCII),
                        col("COLUMN_2", 5, FbEncoding.forName("WIN1252")),
                        EndColumn.require(EndColumn.Type.CRLF)),
                outputResource, ByteOrderType.AUTO);
        return new ExternalTableWriter(externalTable);
    }

}