// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FbVarcharTest {

    // Restrict to printable ASCII
    private static final RandomStringGenerator randomStringGenerator =
            RandomStringGenerator.builder().withinRange('!', '~').get();

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            length, encodingName, valueLength, expectedPadding
            5,      ASCII,        5,           0
            5,      ASCII,        2,           3
            5,      ASCII,        0,           5
            5,      UTF8,         5,           15
            5,      UTF8,         2,           18
            5,      UTF8,         0,           20
            """)
    void testWriteValue(int length, String encodingName, int valueLength, int expectedPadding) throws Exception {
        var fbEncoding = FbEncoding.forName(encodingName);
        Charset charset = fbEncoding.charset();
        var instance = new FbVarchar(length, fbEncoding);
        final String stringValue = randomStringGenerator.generate(valueLength);
        final byte[] expectedBytes = stringValue.getBytes(charset);

        byte[] writtenBytes = writeAndGetBytes(stringValue, instance);

        assertEquals(2 + instance.maxByteLength(), writtenBytes.length, "Unexpected length of writtenBytes");
        var buf = ByteBuffer.wrap(writtenBytes).order(ByteOrder.nativeOrder());
        assertEquals(expectedBytes.length, buf.getShort(), "Unexpected actual value length");
        byte[] valueBytes = new byte[expectedBytes.length];
        buf.get(valueBytes);
        assertArrayEquals(expectedBytes, valueBytes, "Expected value and actual value do not match");
        assertEquals(expectedPadding, buf.remaining(), "Unexpected remaining bytes of padding");
        if (expectedPadding > 0) {
            byte[] paddingBytes = new byte[expectedPadding];
            buf.get(paddingBytes);
            assertArrayEquals(new byte[expectedPadding], paddingBytes, "Expected all padding bytes to be 0x00");
        }
    }

    private byte[] writeAndGetBytes(String value, FbVarchar varchar) throws IOException {
        var baos = new ByteArrayOutputStream(varchar.maxByteLength() + 2);
        varchar.writeValue(value, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        return baos.toByteArray();
    }

}
