// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.EndColumn.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.emptyList;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.bigint;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.col;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.integer;
import static nl.lawinegevaar.exttablegen.ColumnFixtures.smallint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalTableTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void nullEmptyOrBlankNameReplacedWithDefaultName(String name) {
        var table = new ExternalTable(
                name, List.of(col("COLUMN1", 10, FbEncoding.ASCII)), null, ByteOrderType.AUTO);

        assertEquals("DEFAULT_EXTERNAL_TABLE_NAME", table.name(), "unexpected table name");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            tablename, tablename, "tablename"
            "tablename", "tablename", "tablename"
            # name is trimmed
            ' tablename ', tablename, "tablename"
            """)
    void nameAndQuotedName(String name, String expectedName, String expectedQuotedName) {
        var table = new ExternalTable(
                name, List.of(col("COLUMN1", 10, FbEncoding.ASCII)), null, ByteOrderType.AUTO);

        assertEquals(expectedName, table.name(), "unexpected name");
        assertEquals(expectedQuotedName, table.quotedName(), "unexpected quotedName");
    }

    @Test
    void disallowEmptyColumnList() {
        assertThrows(IllegalArgumentException.class, () ->
                        new ExternalTable(null, emptyList(), null, ByteOrderType.AUTO),
                "should disallow instantiation with empty list");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            true,  false, false
            false, true,  false
            true,  false, true
            false, true,  true
            true,  true,  true
            """)
    void disallowEndColumnBeforeEndOfList(boolean column1End, boolean column2End, boolean column3End) {
        var columns = List.of(
                column1End ? EndColumn.require(Type.CRLF) : col("COLUMN1", 10, FbEncoding.ASCII),
                column2End ? EndColumn.require(Type.CRLF) : col("COLUMN2", 10, FbEncoding.ASCII),
                column3End ? EndColumn.require(Type.CRLF) : col("COLUMN3", 10, FbEncoding.ASCII));
        assertThrows(IllegalArgumentException.class, () -> new ExternalTable(null, columns, null, ByteOrderType.AUTO),
                "should disallow instantiation with list containing EndColumn before the last item");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void toCreateTableStatement(boolean useNullFile) {
        OutputResource outputResource = useNullFile
                ? OutputResource.nullOutputResource()
                : OutputResource.of(Path.of("example", "path", "external.dat"));
        var table = new ExternalTable("EXAMPLE_TABLE",
                List.of(
                        col("COLUMN1", 10, FbEncoding.ASCII),
                        col("COLUMN2", 25, FbEncoding.forName("WIN1252")),
                        col("COLUMN3", 50, FbEncoding.UTF8)),
                outputResource, ByteOrderType.AUTO);

        String expectedCreateTable = """
                create table "EXAMPLE_TABLE" external file %s (
                  "COLUMN1" char(10) character set ASCII,
                  "COLUMN2" char(25) character set WIN1252,
                  "COLUMN3" char(50) character set UTF8
                );
                """
                .formatted(outputResource.path()
                        .map(Object::toString)
                        .map(SqlSyntaxUtils::enquoteLiteral)
                        .orElse("'##REPLACE_ME##'"));

        assertEquals(expectedCreateTable, table.toCreateTableStatement());
    }

    @Test
    void deriveFromWithHeader(@TempDir Path tempDir) {
        Path extTablePath = tempDir.resolve("deriveFromWithHeader.dat");
        var inputResource = InputResource.of("""
                        first column,second column
                        12,1234
                        123,12
                        1234,12345
                        """,
                ISO_8859_1);
        var csvFile = new CsvFile(inputResource, new CsvFile.Config(ISO_8859_1, 0, true));
        var tableConfig = new ExternalTable.Config("WITH_HEADER", extTablePath, FbEncoding.ISO8859_1, Type.CRLF,
                ByteOrderType.AUTO);

        var externalTable = ExternalTable.deriveFrom(csvFile, tableConfig);

        String expectedCreateTable = """
                create table "WITH_HEADER" external file %s (
                  "first column" char(4) character set ISO8859_1,
                  "second column" char(5) character set ISO8859_1,
                  "CRLF" char(2) character set ASCII default _ASCII x'0d0a'
                );
                """.formatted(SqlSyntaxUtils.enquoteLiteral(extTablePath.toString()));

        assertEquals(expectedCreateTable, externalTable.toCreateTableStatement());
    }

    @Test
    void deriveFromWithoutHeader(@TempDir Path tempDir) {
        Path extTablePath = tempDir.resolve("deriveFromWithoutHeader.dat");
        var inputResource = InputResource.of("""
                        12,1234
                        123,12
                        1234,12345
                        """,
                ISO_8859_1);
        var csvFile = new CsvFile(inputResource, new CsvFile.Config(ISO_8859_1, 0, false));
        var tableConfig = new ExternalTable.Config("WITHOUT_HEADER", extTablePath, FbEncoding.ISO8859_1, Type.NONE,
                ByteOrderType.AUTO);

        var externalTable = ExternalTable.deriveFrom(csvFile, tableConfig);

        String expectedCreateTable = """
                create table "WITHOUT_HEADER" external file %s (
                  "COLUMN_1" char(4) character set ISO8859_1,
                  "COLUMN_2" char(5) character set ISO8859_1
                );
                """.formatted(SqlSyntaxUtils.enquoteLiteral(extTablePath.toString()));

        assertEquals(expectedCreateTable, externalTable.toCreateTableStatement());
    }

    @Test
    void deriveFromEmptyFile(@TempDir Path tempDir) {
        Path extTablePath = tempDir.resolve("deriveFromEmptyFile.dat");
        var inputResource = InputResource.of(new byte[0]);
        var csvFile = new CsvFile(inputResource, new CsvFile.Config(ISO_8859_1, 0, false));
        var tableConfig = new ExternalTable.Config("WITHOUT_HEADER", extTablePath, FbEncoding.ISO8859_1, Type.CRLF,
                ByteOrderType.AUTO);

        assertThrows(NoColumnNamesException.class, () -> ExternalTable.deriveFrom(csvFile, tableConfig));
    }

    @Test
    void toCreateTableStatement_smallint() {
        var table = new ExternalTable("EXAMPLE_TABLE",
                List.of(
                        col("COLUMN1", 10, FbEncoding.ASCII),
                        col("COLUMN2", 25, FbEncoding.forName("WIN1252")),
                        col("COLUMN3", 50, FbEncoding.UTF8),
                        smallint("COLUMN4")),
                OutputResource.nullOutputResource(), ByteOrderType.AUTO);

        String expectedCreateTable =
                """
                create table "EXAMPLE_TABLE" external file '##REPLACE_ME##' (
                  "COLUMN1" char(10) character set ASCII,
                  "COLUMN2" char(25) character set WIN1252,
                  "COLUMN3" char(50) character set UTF8,
                  "COLUMN4" smallint
                );
                """;

        assertEquals(expectedCreateTable, table.toCreateTableStatement());
    }

    @Test
    void toCreateTableStatement_integer() {
        var table = new ExternalTable("EXAMPLE_TABLE",
                List.of(
                        col("COLUMN1", 10, FbEncoding.ASCII),
                        col("COLUMN2", 25, FbEncoding.forName("WIN1252")),
                        col("COLUMN3", 50, FbEncoding.UTF8),
                        integer("COLUMN4")),
                OutputResource.nullOutputResource(), ByteOrderType.AUTO);

        String expectedCreateTable =
                """
                create table "EXAMPLE_TABLE" external file '##REPLACE_ME##' (
                  "COLUMN1" char(10) character set ASCII,
                  "COLUMN2" char(25) character set WIN1252,
                  "COLUMN3" char(50) character set UTF8,
                  "COLUMN4" integer
                );
                """;

        assertEquals(expectedCreateTable, table.toCreateTableStatement());
    }

    @Test
    void toCreateTableStatement_bigint() {
        var table = new ExternalTable("EXAMPLE_TABLE",
                List.of(
                        col("COLUMN1", 10, FbEncoding.ASCII),
                        col("COLUMN2", 25, FbEncoding.forName("WIN1252")),
                        col("COLUMN3", 50, FbEncoding.UTF8),
                        bigint("COLUMN4")),
                OutputResource.nullOutputResource(), ByteOrderType.AUTO);

        String expectedCreateTable =
                """
                create table "EXAMPLE_TABLE" external file '##REPLACE_ME##' (
                  "COLUMN1" char(10) character set ASCII,
                  "COLUMN2" char(25) character set WIN1252,
                  "COLUMN3" char(50) character set UTF8,
                  "COLUMN4" bigint
                );
                """;

        assertEquals(expectedCreateTable, table.toCreateTableStatement());
    }

}