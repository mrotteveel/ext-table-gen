// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
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
            BIG_ENDIAN,    fff0
            BIG_ENDIAN,    0fff
            LITTLE_ENDIAN, 007f
            LITTLE_ENDIAN, 7f00
            LITTLE_ENDIAN, ff00
            LITTLE_ENDIAN, 00ff
            LITTLE_ENDIAN, fff0
            LITTLE_ENDIAN, 0fff
            AUTO,          007f
            AUTO,          7f00
            AUTO,          ff00
            AUTO,          00ff
            AUTO,          fff0
            AUTO,          0fff
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

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            byteOrder,     value
            BIG_ENDIAN,    0000007f
            BIG_ENDIAN,    7f000000
            BIG_ENDIAN,    ff000000
            BIG_ENDIAN,    000000ff
            BIG_ENDIAN,    fffffff0
            BIG_ENDIAN,    0fffffff
            LITTLE_ENDIAN, 0000007f
            LITTLE_ENDIAN, 7f000000
            LITTLE_ENDIAN, ff000000
            LITTLE_ENDIAN, 000000ff
            LITTLE_ENDIAN, fffffff0
            LITTLE_ENDIAN, 0fffffff
            AUTO,          0000007f
            AUTO,          7f000000
            AUTO,          ff000000
            AUTO,          000000ff
            AUTO,          fffffff0
            AUTO,          0fffffff
            """)
    void testWriteInt(ByteOrderType byteOrderType, String valueString) throws Exception {
        int value = (int) Long.parseLong(valueString, 16);
        var baos = new ByteArrayOutputStream();
        var encoder = EncoderOutputStream.of(byteOrderType).with(baos);

        encoder.writeInt(value);

        var byteBuffer = ByteBuffer.wrap(baos.toByteArray());
        byteBuffer.order(byteOrderType.byteOrder());
        assertEquals(value, byteBuffer.getInt());
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            byteOrder,     value
            BIG_ENDIAN,    000000000000007f
            BIG_ENDIAN,    7f00000000000000
            BIG_ENDIAN,    ff00000000000000
            BIG_ENDIAN,    00000000000000ff
            BIG_ENDIAN,    fffffffffffffff0
            BIG_ENDIAN,    0fffffffffffffff
            LITTLE_ENDIAN, 000000000000007f
            LITTLE_ENDIAN, 7f00000000000000
            LITTLE_ENDIAN, ff00000000000000
            LITTLE_ENDIAN, 00000000000000ff
            LITTLE_ENDIAN, fffffffffffffff0
            LITTLE_ENDIAN, 0fffffffffffffff
            AUTO,          000000000000007f
            AUTO,          7f00000000000000
            AUTO,          ff00000000000000
            AUTO,          00000000000000ff
            AUTO,          fffffffffffffff0
            AUTO,          0fffffffffffffff
            """)
    void testWriteLong(ByteOrderType byteOrderType, String valueString) throws Exception {
        long value = new BigInteger(valueString, 16).longValue();
        var baos = new ByteArrayOutputStream();
        var encoder = EncoderOutputStream.of(byteOrderType).with(baos);

        encoder.writeLong(value);

        var byteBuffer = ByteBuffer.wrap(baos.toByteArray());
        byteBuffer.order(byteOrderType.byteOrder());
        assertEquals(value, byteBuffer.getLong());
    }

}