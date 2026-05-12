// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.apache.commons.lang3.ArrayUtils;
import org.firebirdsql.decimal.Decimal128;
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

class FbDecfloat34Test {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            value,                 onOverflow,        expectedValue
            1,                     THROW_EXCEPTION,   1
            1234567890123456,      THROW_EXCEPTION,   1234567890123456
            12345678901234567,     THROW_EXCEPTION,   12345678901234567
            9999999999999999E369,  THROW_EXCEPTION,   9999999999999999E369
            9999999999999999E370,  ROUND_TO_INFINITY, 9999999999999999E370
            9999999999999999E370,  THROW_EXCEPTION,   9999999999999999E370
            -9999999999999999E369, THROW_EXCEPTION,   -9999999999999999E369
            -9999999999999999E370, ROUND_TO_INFINITY, -9999999999999999E370
            -9999999999999999E370, THROW_EXCEPTION,   -9999999999999999E370
            1E-398,                THROW_EXCEPTION,   1E-398
            1E-399,                THROW_EXCEPTION,   1E-399
            1234567890123456789012345678901234,       THROW_EXCEPTION,   1234567890123456789012345678901234
            12345678901234567890123456789012345,      THROW_EXCEPTION,   1.234567890123456789012345678901234E34
            9999999999999999999999999999999999E6111,  THROW_EXCEPTION,   9999999999999999999999999999999999E6111
            9999999999999999999999999999999999E6111,  ROUND_TO_INFINITY, 9999999999999999999999999999999999E6111
            9999999999999999999999999999999999E6112,  ROUND_TO_INFINITY, +Infinity
            -9999999999999999999999999999999999E6111, THROW_EXCEPTION,   -9999999999999999999999999999999999E6111
            -9999999999999999999999999999999999E6111, ROUND_TO_INFINITY, -9999999999999999999999999999999999E6111
            -9999999999999999999999999999999999E6112, ROUND_TO_INFINITY, -Infinity
            1E-6176,                                  THROW_EXCEPTION,   1E-6176
            1E-6177,                                  THROW_EXCEPTION,   0E-6176
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
        var instance = new FbDecfloat34(onOverflow);
        var expectedDecimal = Decimal128.valueOf(expectedValue);

        assertEquals(expectedDecimal, writeAndGetValue(instance, value));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            value,                                    onOverflow,        expectedExceptionName
            NOT_A_NUMBER,                             ROUND_TO_INFINITY, NumberFormatException
            9999999999999999999999999999999999E6112,  THROW_EXCEPTION,   DecimalOverflowException
            -9999999999999999999999999999999999E6112, THROW_EXCEPTION,   DecimalOverflowException
            """)
    void testWriteInvalidValue(String value, DecfloatOnOverflow onOverflow, String expectedExceptionName) {
        var instance = new FbDecfloat34(onOverflow);

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
        assertEquals(Decimal128.valueOf(BigDecimal.ZERO),
                writeAndGetValue(new FbDecfloat34(DecfloatOnOverflow.THROW_EXCEPTION), valueToWrite));
    }

    @Test
    void testWriteEmpty() throws Exception {
        var baos = new ByteArrayOutputStream();
        new FbDecfloat34(DecfloatOnOverflow.THROW_EXCEPTION)
                .writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        assertEquals(Decimal128.valueOf(BigDecimal.ZERO), toDecimal128(baos.toByteArray()));
    }

    @Test
    void testWriteValue_nonNullConverter() throws Exception {
        var instance = new FbDecfloat34(DecfloatOnOverflow.THROW_EXCEPTION);
        var normalConverter = Converter.parseBigDecimal(Locale.US);
        var offsetByOneConverter = Converter.of(BigDecimal.class, v -> normalConverter.convert(v).add(BigDecimal.ONE));

        assertEquals(Decimal128.valueOf(BigDecimal.TEN), writeAndGetValue(instance, "9", offsetByOneConverter));
    }

    private static Decimal128 writeAndGetValue(FbDecfloat34 decfloatType, String valueToWrite) throws IOException {
        return writeAndGetValue(decfloatType, valueToWrite, null);
    }

    private static Decimal128 writeAndGetValue(FbDecfloat34 decfloatType, String valueToWrite,
            @Nullable Converter<BigDecimal> bigDecimalConverter) throws IOException {
        var baos = new ByteArrayOutputStream();
        decfloatType.withConverterChecked(bigDecimalConverter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        return toDecimal128(baos.toByteArray());
    }

    private static Decimal128 toDecimal128(byte[] bytes) {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ArrayUtils.reverse(bytes);
        }
        return Decimal128.parseBytes(bytes);
    }

}
