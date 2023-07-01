// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class EncoderOutputStreamTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            byteOrder,     value
            BIG_ENDIAN,    007f
            BIG_ENDIAN,    7f00
            BIG_ENDIAN,    ff00
            BIG_ENDIAN,    00ff
            LITTLE_ENDIAN, 007f
            LITTLE_ENDIAN, 7f00
            LITTLE_ENDIAN, ff00
            LITTLE_ENDIAN, 00ff
            AUTO,          007f
            AUTO,          7f00
            AUTO,          ff00
            AUTO,          00ff
            """)
    void testWriteShort(ByteOrderType byteOrderType, String valueString) throws Exception {
        short value = (short) Integer.parseInt(valueString, 16);
        var baos = new ByteArrayOutputStream();
        var encoder = EncoderOutputStream.of(byteOrderType).with(baos);

        encoder.writeShort(value);

        var byteBuffer = ByteBuffer.wrap(baos.toByteArray());
        byteBuffer.order(byteOrderType.byteOrder());
        assertEquals(value, byteBuffer.getShort());
    }

}