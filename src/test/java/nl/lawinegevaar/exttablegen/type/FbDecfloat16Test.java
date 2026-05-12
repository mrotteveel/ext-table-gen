// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.apache.commons.lang3.ArrayUtils;
import org.firebirdsql.decimal.Decimal64;
import org.firebirdsql.decimal.DecimalOverflowException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.shadow.de.siegmar.fastcsv.util.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbDecfloat16Test {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            value,                 onOverflow,        expectedValue
            1,                     THROW_EXCEPTION,   1
            1234567890123456,      THROW_EXCEPTION,   1234567890123456
            12345678901234567,     THROW_EXCEPTION,   1.234567890123457E16
            9999999999999999E369,  THROW_EXCEPTION,   9999999999999999E369
            9999999999999999E370,  ROUND_TO_INFINITY, +Infinity
            -9999999999999999E369, THROW_EXCEPTION,   -9999999999999999E369
            -9999999999999999E370, ROUND_TO_INFINITY, -Infinity
            1E-398,                THROW_EXCEPTION,   1E-398
            1E-399,                THROW_EXCEPTION,   0E-398
            1234567890123456789012345678901234,       THROW_EXCEPTION,   1.234567890123457E33
            12345678901234567890123456789012345,      THROW_EXCEPTION,   1.234567890123457E34
            Infinity,              THROW_EXCEPTION,   +Infinity
            +Infinity,             THROW_EXCEPTION,   +Infinity
            -Infinity,             THROW_EXCEPTION,   -Infinity
            NaN,                   THROW_EXCEPTION,   +NaN
            +NaN,                  THROW_EXCEPTION,   +NaN
            -NaN,                  THROW_EXCEPTION,   -NaN
            sNaN,                  THROW_EXCEPTION,   +sNaN
            +sNaN,                 THROW_EXCEPTION,   +sNaN
            -sNaN,                 THROW_EXCEPTION,   -sNaN
            """)
    void testWriteValue(String value, DecfloatOnOverflow onOverflow, String expectedValue) throws Exception {
        var instance = new FbDecfloat16(onOverflow);
        var expectedDecimal = Decimal64.valueOf(expectedValue);

        assertEquals(expectedDecimal, writeAndGetValue(instance, value));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            value,                 onOverflow,        expectedExceptionName
            NOT_A_NUMBER,          ROUND_TO_INFINITY, NumberFormatException
            9999999999999999E370,  THROW_EXCEPTION,   DecimalOverflowException
            -9999999999999999E370, THROW_EXCEPTION,   DecimalOverflowException
            """)
    void testWriteInvalidValue(String value, DecfloatOnOverflow onOverflow, String expectedExceptionName) {
        var instance = new FbDecfloat16(onOverflow);

        Class<? extends RuntimeException> expectedException = switch (expectedExceptionName) {
            case "NumberFormatException" -> NumberFormatException.class;
            case "DecimalOverflowException" -> DecimalOverflowException.class;
            default -> throw new AssertionError("Unexpected exception name: " + expectedExceptionName);
        };

        assertThrows(expectedException, () -> writeAndGetValue(instance, value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testWriteValue_nullOrEmpty(String valueToWrite) throws Exception {
        assertEquals(Decimal64.valueOf(BigDecimal.ZERO),
                writeAndGetValue(new FbDecfloat16(DecfloatOnOverflow.THROW_EXCEPTION), valueToWrite));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        new FbDecfloat16(DecfloatOnOverflow.THROW_EXCEPTION)
                .writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        assertEquals(Decimal64.valueOf(BigDecimal.ZERO), toDecimal64(baos.toByteArray()));
    }

    @Test
    void testWriteValue_nonNullConverter() throws Exception {
        var instance = new FbDecfloat16(DecfloatOnOverflow.THROW_EXCEPTION);
        var normalConverter = Converter.parseBigDecimal(Locale.US);
        var offsetByOneConverter = Converter.of(BigDecimal.class, v -> normalConverter.convert(v).add(BigDecimal.ONE));

        assertEquals(Decimal64.valueOf(BigDecimal.TEN), writeAndGetValue(instance, "9", offsetByOneConverter));
    }

    private static Decimal64 writeAndGetValue(FbDecfloat16 decfloatType, String valueToWrite) throws IOException {
        return writeAndGetValue(decfloatType, valueToWrite, null);
    }

    private static Decimal64 writeAndGetValue(FbDecfloat16 decfloatType, String valueToWrite,
            @Nullable Converter<BigDecimal> bigDecimalConverter) throws IOException {
        var baos = new ByteArrayOutputStream();
        decfloatType.withConverterChecked(bigDecimalConverter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        return toDecimal64(baos.toByteArray());
    }

    private static Decimal64 toDecimal64(byte[] bytes) {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ArrayUtils.reverse(bytes);
        }
        return Decimal64.parseBytes(bytes);
    }

}
