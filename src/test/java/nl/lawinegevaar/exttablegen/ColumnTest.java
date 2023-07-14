// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbChar;
import nl.lawinegevaar.exttablegen.type.FbEncoding;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ColumnTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void nullEmptyOrBlankNameNotAllowed(String name) {
        assertThrows(IllegalArgumentException.class, () -> new Column(name, new FbChar(10, FbEncoding.ASCII)));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            columnname, columnname, "columnname"
            "columnname", "columnname", "columnname"
            # name is trimmed
            ' columnname ', columnname, "columnname"
            """)
    void nameAndQuotedName(String name, String expectedName, String expectedQuotedName) {
        var column = new Column(name, new FbChar(10, FbEncoding.ASCII));

        assertEquals(expectedName, column.name(), "unexpected name");
        assertEquals(expectedQuotedName, column.quotedName(), "unexpected quotedName");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            5,  ASCII, 5
            10, UTF8,  40
            """)
    void writeEmpty(int length, String fbEncodingName, int expectedLength) throws Exception {
        var column = new Column("dummy", new FbChar(length, FbEncoding.forName(fbEncodingName)));
        byte[] expectedBytes = new byte[expectedLength];
        Arrays.fill(expectedBytes, (byte) 0x20);

        var baos = new ByteArrayOutputStream();
        column.writeEmpty(EncoderOutputStream.of(ByteOrderType.LITTLE_ENDIAN).withColumnCount(1).writeTo(baos));
        byte[] result = baos.toByteArray();
        assertArrayEquals(expectedBytes, result, "bytes");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            5, ASCII, A,            A,            4
            5, ASCII, ABCDE,        ABCDE,        0
            1, UTF8,  \uD83D\uDE00, \uD83D\uDE00, 0
            1, UTF8,  A,            A,            3
            5, UTF8,  ABCDE,        ABCDE,        15
            5, UTF8,  ABCDEF,       ABCDE,        15
            5, UTF8,  ABCD\u00e9,   ABCD\u00e9,   14
            5, ASCII, ABCD\u00e9,   ABCD?,        0
            2, UTF8,  \uD83D\uDE00\uD83D\uDE00, \uD83D\uDE00\uD83D\uDE00, 0
            # Checks for an IndexOutOfBoundsException which occurred in an earlier implementation
            3, UTF8,  \uD83D\uDE00\uD83D\uDE00, \uD83D\uDE00\uD83D\uDE00, 4
            """)
    void writeValue(int length, String fbEncodingName, String inputValue, String outputValueTrimmed,
            int trailingSpaces) throws Exception {
        var encoding = FbEncoding.forName(fbEncodingName);
        var column = new Column("dummy", new FbChar(length, encoding));
        String expectedValue = outputValueTrimmed + (" ".repeat(trailingSpaces));

        var baos = new ByteArrayOutputStream();
        column.writeValue(inputValue,
                EncoderOutputStream.of(ByteOrderType.LITTLE_ENDIAN).withColumnCount(1).writeTo(baos));
        assertArrayEquals(expectedValue.getBytes(encoding.charset()), baos.toByteArray());
        assertEquals(expectedValue, baos.toString(encoding.charset()));
    }
}