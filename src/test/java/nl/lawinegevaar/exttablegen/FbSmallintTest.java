// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbSmallintTest {

    private static final FbSmallint smallintType = new FbSmallint();

    @ParameterizedTest
    @ValueSource(strings = { Short.MIN_VALUE + "", "-1", "0", "1", Short.MAX_VALUE + "" })
    void testWriteValue(String shortString) throws Exception {
        short expectedValue = Short.parseShort(shortString);

        assertEquals(expectedValue, writeAndGetValue(shortString));
    }

    @ParameterizedTest
    @ValueSource(strings = { (Short.MIN_VALUE - 1) + "", (Short.MAX_VALUE + 1) + "", "NOT_A_NUMBER", "FF" })
    void testWriteValue_outOfRangeOrInvalid_throwsNumberFormatException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        assertThrows(NumberFormatException.class, () ->
                smallintType.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(0, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        smallintType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        assertEquals(0, buf.getShort());
    }

    @Test
    void testHashCode() {
        assertEquals(smallintType.hashCode(), new FbSmallint().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(smallintType, new FbSmallint());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(smallintType, new FbChar(10, FbEncoding.ASCII));
    }

    short writeAndGetValue(String valueToWrite) throws IOException {
        var baos = new ByteArrayOutputStream();
        smallintType.writeValue(valueToWrite,
                EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        return buf.getShort();
    }

}