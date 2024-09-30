// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.DoubleConverter;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbDoublePrecisionTest {

    private static final FbDoublePrecision doublePrecisionType = new FbDoublePrecision();

    @ParameterizedTest
    @ValueSource(doubles = { Double.POSITIVE_INFINITY, Double.MAX_VALUE, 1, Double.MIN_VALUE, 0d,
            -Double.MIN_VALUE, -1d, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.NaN })
    void testWriteValue(double value) throws IOException {
        assertEquals(value, writeAndGetValue(String.valueOf(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "NOT_A_NUMBER", "FF" })
    void testWriteValue_invalid_throwsNumberFormatException(String valueToWrite) {
        EncoderOutputStream out = EncoderOutputStream.of(ByteOrderType.AUTO)
                .withColumnCount(1)
                .writeTo(new ByteArrayOutputStream());
        assertThrows(NumberFormatException.class, () -> doublePrecisionType.writeValue(valueToWrite, out));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(0d, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var normalConverter = DoubleConverter.wrap(Converter.of(Double.class, Double::valueOf));
        assertNotNull(normalConverter);
        var offsetByTwoConverter = Converter.of(Double.class, v -> normalConverter.convertToDouble(v) + 2.5);
        assertEquals(5.2, writeAndGetValue("2.7", offsetByTwoConverter));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        doublePrecisionType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        assertEquals(0, buf.getInt());
    }

    @Test
    void testHashCode() {
        assertEquals(doublePrecisionType.hashCode(), new FbDoublePrecision().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(doublePrecisionType, new FbDoublePrecision());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(doublePrecisionType, new FbChar(10, FbEncoding.ASCII));
    }

    double writeAndGetValue(String valueToWrite) throws IOException {
        return writeAndGetValue(valueToWrite, null);
    }

    double writeAndGetValue(String valueToWrite, @Nullable Converter<Double> converter) throws IOException {
        var baos = new ByteArrayOutputStream();
        doublePrecisionType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        return buf.getDouble();
    }

}