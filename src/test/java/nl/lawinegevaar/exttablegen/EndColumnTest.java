// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.type.FbChar;
import nl.lawinegevaar.exttablegen.type.FbEncoding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndColumnTest {

    @Test
    void typeNONE_empty() {
        assertTrue(EndColumn.of(EndColumn.Type.NONE).isEmpty(), "expected no EndColumn for NONE");
    }

    @SuppressWarnings("UnnecessaryStringEscape")
    @ParameterizedTest
    @CsvSource(textBlock = """
            LF, '\n', 0a
            CRLF, '\r\n', 0d0a
            """)
    void endColumnOfSupportedType(EndColumn.Type type, String value, String defaultValue) throws Exception {
        int length = value.length();
        var endColumn = EndColumn.of(type)
                .orElseThrow(() -> new AssertionFailedError("Expected EndColumn for LF"));

        assertEquals(type.name(), endColumn.name(), "name");
        assertEquals('"' + type.name() + '"', endColumn.quotedName(), "quotedName");
        var sb = new StringBuilder();
        endColumn.appendColumnDefinition(sb);
        assertEquals("\"%s\" char(%d) character set ASCII default _ASCII x'%s'".formatted(type, length, defaultValue),
                sb.toString(), "column definition");
        assertEquals(new FbChar(length, FbEncoding.ASCII), endColumn.datatype(), "datatype");

        var baos = new ByteArrayOutputStream();
        endColumn.writeValue("anything",
                EncoderOutputStream.of(ByteOrderType.LITTLE_ENDIAN).withColumnCount(1).writeTo(baos));
        assertEquals(value, baos.toString(StandardCharsets.US_ASCII), "writeValue");
        baos.reset();
        endColumn.writeEmpty(EncoderOutputStream.of(ByteOrderType.LITTLE_ENDIAN).withColumnCount(1).writeTo(baos));
        assertEquals(value, baos.toString(StandardCharsets.US_ASCII), "writeEmpty");
    }

}