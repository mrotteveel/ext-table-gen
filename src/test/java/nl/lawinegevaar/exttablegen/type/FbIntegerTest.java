// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseInteger;
import org.jspecify.annotations.Nullable;
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

class FbIntegerTest {

    private static final FbInteger integerType = new FbInteger();

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE, Integer.MAX_VALUE })
    void testWriteValue(int value) throws Exception {
        assertEquals(value, writeAndGetValue(String.valueOf(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = { (Integer.MIN_VALUE - 1L) + "", (Integer.MAX_VALUE + 1L) + "", "NOT_A_NUMBER", "FF" })
    void testWriteValue_outOfRangeOrInvalid_throwsNumberFormatException(String valueToWrite) {
        EncoderOutputStream out = EncoderOutputStream.of(ByteOrderType.AUTO)
                .withColumnCount(1)
                .writeTo(new ByteArrayOutputStream());
        assertThrows(NumberFormatException.class, () -> integerType.writeValue(valueToWrite, out));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(0, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var normalConverter = ParseInteger.ofRadix(16);
        var offsetByTwoConverter = Converter.of(Integer.class, v -> normalConverter.convertToInt(v) + 2);
        assertEquals(5, writeAndGetValue("3", offsetByTwoConverter));
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
        return writeAndGetValue(valueToWrite, null);
    }

    int writeAndGetValue(String valueToWrite, @Nullable Converter<Integer> converter) throws IOException {
        var baos = new ByteArrayOutputStream();
        integerType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        return buf.getInt();
    }

}