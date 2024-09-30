// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.FloatConverter;
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

class FbFloatTest {

    private static final FbFloat floatType = new FbFloat();

    @ParameterizedTest
    @ValueSource(floats = { Float.POSITIVE_INFINITY, Float.MAX_VALUE, 1f, Float.MIN_VALUE, 0f,
            -Float.MIN_VALUE, -1f, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.NaN })
    void testWriteValue(float value) throws IOException {
        assertEquals(value, writeAndGetValue(String.valueOf(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "NOT_A_NUMBER", "FF" })
    void testWriteValue_invalid_throwsNumberFormatException(String valueToWrite) {
        EncoderOutputStream out = EncoderOutputStream.of(ByteOrderType.AUTO)
                .withColumnCount(1)
                .writeTo(new ByteArrayOutputStream());
        assertThrows(NumberFormatException.class, () -> floatType.writeValue(valueToWrite, out));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(0f, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var normalConverter = FloatConverter.wrap(Converter.of(Float.class, Float::valueOf));
        assertNotNull(normalConverter);
        var offsetByTwoConverter = Converter.of(Float.class, v -> normalConverter.convertToFloat(v) + 2.5f);
        assertEquals(5.2f, writeAndGetValue("2.7", offsetByTwoConverter));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        floatType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        assertEquals(0, buf.getInt());
    }

    @Test
    void testHashCode() {
        assertEquals(floatType.hashCode(), new FbFloat().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(floatType, new FbFloat());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(floatType, new FbChar(10, FbEncoding.ASCII));
    }

    float writeAndGetValue(String valueToWrite) throws IOException {
        return writeAndGetValue(valueToWrite, null);
    }

    float writeAndGetValue(String valueToWrite, @Nullable Converter<Float> converter) throws IOException {
        var baos = new ByteArrayOutputStream();
        floatType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        return buf.getFloat();
    }

}
