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

import static org.junit.jupiter.api.Assertions.*;

class FbIntegerTest {

    private static final FbInteger integerType = new FbInteger();

    @ParameterizedTest
    @ValueSource(strings = { Integer.MIN_VALUE + "", Short.MIN_VALUE + "", "-1", "0", "1", Short.MAX_VALUE + "",
            Integer.MAX_VALUE + "" })
    void testWriteValue(String integerString) throws Exception {
        int expectedValue = Integer.parseInt(integerString);

        assertEquals(expectedValue, writeAndGetValue(integerString));
    }

    @ParameterizedTest
    @ValueSource(strings = { (Integer.MIN_VALUE - 1L) + "", (Integer.MAX_VALUE + 1L) + "", "NOT_A_NUMBER", "FF" })
    void testWriteValue_outOfRangeOrInvalid_throwsNumberFormatException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        assertThrows(NumberFormatException.class, () ->
                integerType.writeValue(valueToWrite,
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
        integerType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        assertEquals(0, buf.getInt());
    }

    @Test
    void testHashCode() {
        assertEquals(integerType.hashCode(), new FbInteger().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(integerType, new FbInteger());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(integerType, new FbChar(10, FbEncoding.ASCII));
    }

    int writeAndGetValue(String valueToWrite) throws IOException {
        var baos = new ByteArrayOutputStream();
        integerType.writeValue(valueToWrite,
                EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        return buf.getInt();
    }

}