// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HexFormat;

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
        var encoder = EncoderOutputStream.of(byteOrderType).withColumnCount(1).writeTo(baos);

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
        var encoder = EncoderOutputStream.of(byteOrderType).withColumnCount(1).writeTo(baos);

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
        var encoder = EncoderOutputStream.of(byteOrderType).withColumnCount(1).writeTo(baos);

        encoder.writeLong(value);

        var byteBuffer = ByteBuffer.wrap(baos.toByteArray());
        byteBuffer.order(byteOrderType.byteOrder());
        assertEquals(value, byteBuffer.getLong());
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            byteOrder,     value
            BIG_ENDIAN,    0000000000000000000000000000007f
            BIG_ENDIAN,    7f000000000000000000000000000000
            BIG_ENDIAN,    ff000000000000000000000000000000
            BIG_ENDIAN,    000000000000000000000000000000ff
            BIG_ENDIAN,    fffffffffffffffffffffffffffffff0
            BIG_ENDIAN,    0fffffffffffffffffffffffffffffff
            LITTLE_ENDIAN, 0000000000000000000000000000007f
            LITTLE_ENDIAN, 7f000000000000000000000000000000
            LITTLE_ENDIAN, ff000000000000000000000000000000
            LITTLE_ENDIAN, 000000000000000000000000000000ff
            LITTLE_ENDIAN, fffffffffffffffffffffffffffffff0
            LITTLE_ENDIAN, 0fffffffffffffffffffffffffffffff
            AUTO,          0000000000000000000000000000007f
            AUTO,          7f000000000000000000000000000000
            AUTO,          ff000000000000000000000000000000
            AUTO,          000000000000000000000000000000ff
            AUTO,          fffffffffffffffffffffffffffffff0
            AUTO,          0fffffffffffffffffffffffffffffff
            """)
    void testWriteInt128(ByteOrderType byteOrderType, String inputHex) throws Exception {
        var value = new BigInteger(HexFormat.of().parseHex(inputHex));
        var baos = new ByteArrayOutputStream();
        var encoder = EncoderOutputStream.of(byteOrderType).withColumnCount(1).writeTo(baos);

        encoder.writeInt128(value);

        byte[] bytes = baos.toByteArray();
        assertEquals(16, bytes.length);
        if (byteOrderType.effectiveValue() == ByteOrderType.LITTLE_ENDIAN) {
            ArrayUtils.reverse(bytes);
        }
        assertEquals(value, new BigInteger(bytes));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            columnCount, writtenBytes, alignment, expectedSize
            1,  0,  1, 0
            1,  1,  1, 1
            1,  0,  2, 0
            1,  1,  2, 2
            1,  2,  2, 2
            1,  3,  2, 4
            1,  0,  4, 0
            1,  1,  4, 4
            1,  2,  4, 4
            1,  3,  4, 4
            1,  4,  4, 4
            1,  5,  4, 8
            # Alignment of first column is not written, but is taken into account for further alignment
            1,  0,  5, 0
            1,  1,  5, 1
            1,  2,  5, 6
            1,  0,  8, 0
            1,  1,  8, 4
            1,  2,  8, 4
            1,  3,  8, 4
            1,  4,  8, 4
            1,  5,  8, 12
            1,  6,  8, 12
            1,  7,  8, 12
            1,  8,  8, 12
            1,  9,  8, 12
            1,  10, 8, 12
            1,  11, 8, 12
            1,  12, 8, 12
            1,  13, 8, 20
            33, 0,  8, 0
            33, 1,  8, 8
            33, 2,  8, 8
            33, 3,  8, 8
            33, 4,  8, 8
            33, 5,  8, 8
            33, 6,  8, 8
            33, 7,  8, 8
            33, 8,  8, 8
            33, 9,  8, 16
            """)
    void testAlignmentBehaviour(int columnCount, int writtenBytes, int alignment, int expectedSize) throws Exception {
        var baos = new ByteArrayOutputStream();
        var encoder = EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(columnCount).writeTo(baos);
        encoder.startRow();
        if (writtenBytes > 0) {
            encoder.write(new byte[writtenBytes]);
        }
        encoder.align(alignment);
        assertEquals(expectedSize, baos.size());
    }

    /**
     * Rationale: Alignment of first column is not written, but is taken into account for further alignment
     */
    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            columnCount, firstAlign, expectedResult
            1,  1, ffffffffffffffff61000000ffffffffffffffff
            1,  2, ffffffffffffffff61000000ffffffffffffffff
            1,  3, ffffffffffffffff6100ffffffffffffffff
            1,  4, ffffffffffffffff61000000ffffffffffffffff
            1,  5, ffffffffffffffff610000ffffffffffffffff
            1,  6, ffffffffffffffff6100ffffffffffffffff
            1,  7, ffffffffffffffff61ffffffffffffffff
            1,  8, ffffffffffffffff6100000000000000ffffffffffffffff
            32, 1, ffffffffffffffff61000000ffffffffffffffff
            32, 2, ffffffffffffffff61000000ffffffffffffffff
            32, 4, ffffffffffffffff61000000ffffffffffffffff
            32, 8, ffffffffffffffff6100000000000000ffffffffffffffff
            33, 1, ffffffffffffffff6100000000000000ffffffffffffffff
            33, 2, ffffffffffffffff6100000000000000ffffffffffffffff
            33, 3, ffffffffffffffff61000000000000ffffffffffffffff
            33, 4, ffffffffffffffff6100000000000000ffffffffffffffff
            33, 5, ffffffffffffffff610000000000ffffffffffffffff
            33, 6, ffffffffffffffff61000000ffffffffffffffff
            33, 7, ffffffffffffffff6100ffffffffffffffff
            33, 8, ffffffffffffffff6100000000000000ffffffffffffffff
            65, 1, ffffffffffffffff61000000ffffffffffffffff
            65, 8, ffffffffffffffff6100000000000000ffffffffffffffff
            """)
    void testAlignment_firstAlignmentNotWritten(int columnCount, int firstAlign, String expectedResult)
            throws Exception {
        var baos = new ByteArrayOutputStream();
        var encoder = EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(columnCount).writeTo(baos);
        encoder.startRow();
        encoder.align(firstAlign);
        encoder.writeLong(-1);
        encoder.write('a');
        encoder.align(8);
        encoder.writeLong(-1);

        assertEquals(expectedResult, HexFormat.of().formatHex(baos.toByteArray()));
    }

}