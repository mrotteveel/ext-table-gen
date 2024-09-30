// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseBigint;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbBigintTest {

    private static final FbBigint bigintType = new FbBigint();

    @ParameterizedTest
    @ValueSource(strings = { Long.MIN_VALUE + "", Integer.MIN_VALUE + "", Short.MIN_VALUE + "", "-1", "0", "1",
            Short.MAX_VALUE + "", Integer.MAX_VALUE + "", Long.MAX_VALUE + "" })
    void testWriteValue(String bigintString) throws Exception {
        long expectedValue = Long.parseLong(bigintString);

        assertEquals(expectedValue, writeAndGetValue(bigintString));
    }

    @ParameterizedTest
    @MethodSource("outOfRangeOrInvalidValues")
    void testWriteValue_outOfRangeOrInvalid_throwsNumberFormatException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        assertThrows(NumberFormatException.class, () ->
                bigintType.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
    }

    static Stream<String> outOfRangeOrInvalidValues() {
        return Stream.of(
                BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE).toString(),
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString(),
                "NOT_A_NUMBER",
                "FF");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(0, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var normalConverter = ParseBigint.ofRadix(16);
        var offsetByTwoConverter = Converter.of(Long.class, v -> normalConverter.convertToLong(v) + 2);
        assertEquals(5L, writeAndGetValue("3", offsetByTwoConverter));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        bigintType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        assertEquals(0, buf.getLong());
    }

    @Test
    void testHashCode() {
        assertEquals(bigintType.hashCode(), new FbBigint().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(bigintType, new FbBigint());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(bigintType, new FbChar(10, FbEncoding.ASCII));
    }

    long writeAndGetValue(String valueToWrite) throws IOException {
        return writeAndGetValue(valueToWrite, null);
    }

    long writeAndGetValue(String valueToWrite, @Nullable Converter<Long> converter) throws IOException {
        var baos = new ByteArrayOutputStream();
        bigintType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        var buf = ByteBuffer.wrap(baos.toByteArray());
        buf.order(ByteOrder.nativeOrder());
        return buf.getLong();
    }


}