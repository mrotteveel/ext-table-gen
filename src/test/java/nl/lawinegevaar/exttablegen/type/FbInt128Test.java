// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseInt128;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbInt128Test {

    private static final FbInt128 int128Type = new FbInt128();

    @ParameterizedTest
    @MethodSource("inRangeValues")
    void testWriteValue(String bigIntegerString) throws Exception {
        var expectedValue = new BigInteger(bigIntegerString);

        assertEquals(expectedValue, writeAndGetValue(bigIntegerString));
    }

    static Stream<String> inRangeValues() {
        return Stream.of(
                FbInt128.MIN_VALUE.toString(),
                Long.MIN_VALUE + "",
                Integer.MIN_VALUE + "",
                Short.MIN_VALUE + "",
                "-1",
                "0",
                "1",
                Short.MAX_VALUE + "",
                Integer.MAX_VALUE + "",
                Long.MAX_VALUE + "",
                FbInt128.MAX_VALUE.toString());
    }

    @ParameterizedTest
    @MethodSource("outOfRangeOrInvalidValues")
    void testWriteValue_outOfRangeOrInvalid_throwsNumberFormatException(String valueToWrite) {
        var baos = new ByteArrayOutputStream();
        assertThrows(NumberFormatException.class, () ->
                int128Type.writeValue(valueToWrite,
                        EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos)));
    }

    static Stream<String> outOfRangeOrInvalidValues() {
        return Stream.of(
                FbInt128.MIN_VALUE.subtract(BigInteger.ONE).toString(),
                FbInt128.MAX_VALUE.add(BigInteger.ONE).toString(),
                "NOT_A_NUMBER",
                "FF");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(BigInteger.ZERO, writeAndGetValue(valueToWrite));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var normalConverter = ParseInt128.ofRadix(16);
        var offsetByTwoConverter = Converter.of(BigInteger.class, v -> normalConverter.convert(v).add(BigInteger.TWO));
        assertEquals(BigInteger.valueOf(5), writeAndGetValue("3", offsetByTwoConverter));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        int128Type.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        byte[] bytes = baos.toByteArray();
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ArrayUtils.reverse(bytes);
        }
        assertEquals(BigInteger.ZERO, new BigInteger(bytes));
    }

    @Test
    void testHashCode() {
        assertEquals(int128Type.hashCode(), new FbInt128().hashCode());
    }

    @Test
    void testEquals() {
        assertEquals(int128Type, new FbInt128());
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(int128Type, new FbChar(10, FbEncoding.ASCII));
    }

    BigInteger writeAndGetValue(String valueToWrite) throws IOException {
        return writeAndGetValue(valueToWrite, null);
    }

    BigInteger writeAndGetValue(String valueToWrite, @Nullable Converter<BigInteger> converter) throws IOException {
        var baos = new ByteArrayOutputStream();
        int128Type.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        byte[] bytes = baos.toByteArray();
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ArrayUtils.reverse(bytes);
        }
        return new BigInteger(bytes);
    }

}