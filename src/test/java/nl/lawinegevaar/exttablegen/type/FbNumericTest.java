// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.ByteOrderType;
import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FbNumericTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            precision, scale, roundingMode, valueToWrite, expectedUnscaledValue
            4,         2,     HALF_UP,      2.345,        235
            4,         2,     HALF_UP,      2.335,        234
            4,         2,     HALF_EVEN,    2.345,        234
            4,         2,     HALF_EVEN,    2.335,        234
            4,         2,     HALF_DOWN,    2.345,        234
            4,         2,     HALF_DOWN,    2.335,        233
            9,         2,     UP,           1.111,        112
            9,         2,     UP,           -1.111,       -112
            9,         2,     DOWN,         1.111,        111
            9,         2,     DOWN,         -1.111,       -111
            9,         2,     CEILING,      1.111,        112
            9,         2,     CEILING,      -1.111,       -111
            9,         2,     FLOOR,        1.111,        111
            9,         2,     FLOOR,        -1.111,       -112
            # writes 0 for null
            9,         0,     UNNECESSARY,  ,             0
            # writes 0 for empty string
            9,         0,     UNNECESSARY,  '',           0
            # limits of the SHORT backing type
            1,         0,     UNNECESSARY,  32767,        32767
            1,         0,     UNNECESSARY,  -32768,       -32768
            4,         4,     UNNECESSARY,  3.2767,       32767
            4,         4,     UNNECESSARY,  -3.2768,      -32768
            # limits of the INTEGER backing type
            5,         0,     UNNECESSARY,  2147483647,  2147483647
            5,         0,     UNNECESSARY,  -2147483648, -2147483648
            9,         9,     UNNECESSARY,  2.147483647,  2147483647
            9,         9,     UNNECESSARY,  -2.147483648, -2147483648
            # limits of the BIGINT backing type
            10,        0,     UNNECESSARY,  9223372036854775807,   9223372036854775807
            10,        0,     UNNECESSARY,  -9223372036854775808,  -9223372036854775808
            18,        18,    UNNECESSARY,  9.223372036854775807,  9223372036854775807
            18,        18,    UNNECESSARY,  -9.223372036854775808, -9223372036854775808
            # limits of the INT128 backing type
            19,        0,     UNNECESSARY,  170141183460469231731687303715884105727,   170141183460469231731687303715884105727
            19,        0,     UNNECESSARY,  -170141183460469231731687303715884105728,  -170141183460469231731687303715884105728
            38,        38,    UNNECESSARY,  1.70141183460469231731687303715884105727,  170141183460469231731687303715884105727
            38,        38,    UNNECESSARY,  -1.70141183460469231731687303715884105728, -170141183460469231731687303715884105728
            """)
    void testWriteValue(int precision, int scale, RoundingMode roundingMode, String valueToWrite,
            BigInteger expectedUnscaledValue) throws Exception {
        var numericType = new FbNumeric(precision, scale, roundingMode);

        byte[] encodedData = writeAndGetValue(numericType, valueToWrite);

        getAssertions(precision).assertValue(encodedData, expectedUnscaledValue);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            precision, scale, roundingMode, valueToWrite, expectedExceptionName
            9,         0,     UNNECESSARY,  not a number, NumberFormatException
            # requires rounding
            9,         2,     UNNECESSARY,  1.234,        ArithmeticException
            # exceeds limits of the SHORT backing type
            4,         0,     UNNECESSARY,  32768,        ArithmeticException
            4,         0,     UNNECESSARY,  -32769,       ArithmeticException
            # exceeds limits of the INTEGER backing type
            9,         0,     UNNECESSARY,  2147483648,   ArithmeticException
            9,         0,     UNNECESSARY,  -2147483649,  ArithmeticException
            # exceeds limits of the BIGINT backing type
            18,        0,     UNNECESSARY,  9223372036854775808,  ArithmeticException
            18,        0,     UNNECESSARY,  -9223372036854775809, ArithmeticException
            # exceeds limits of the INT128 backing type
            38,        0,     UNNECESSARY,  170141183460469231731687303715884105728,  NumberFormatException
            38,        0,     UNNECESSARY,  -170141183460469231731687303715884105729, NumberFormatException
            """)
    void testWriteValue_outOfRangeOrInvalidValues(int precision, int scale, RoundingMode roundingMode,
            String valueToWrite, String expectedExceptionName) {
        var numericType = new FbNumeric(precision, scale, roundingMode);

        Class<? extends RuntimeException> expectedException = switch (expectedExceptionName) {
            case "NumberFormatException" -> NumberFormatException.class;
            case "ArithmeticException" -> ArithmeticException.class;
            default -> throw new AssertionError("Unexpected exception name: " + expectedExceptionName);
        };

        assertThrows(expectedException, () -> writeAndGetValue(numericType, valueToWrite));
    }

    @Test
    void testWriteValue_callsNonNullConverter() throws Exception {
        var numericType = new FbNumeric(9, 0, RoundingMode.UNNECESSARY);
        var normalConverter = Converter.parseBigDecimal(Locale.US);
        var offsetByOneConverter = Converter.of(BigDecimal.class, v -> normalConverter.convert(v).add(BigDecimal.ONE));

        byte[] encodedData = writeAndGetValue(numericType, "5", offsetByOneConverter);

        FixedPointBackingTypeAssertions.INTEGER.assertValue(encodedData, BigInteger.valueOf(6));
    }

    @ParameterizedTest
    @ValueSource(ints = { 4, 9, 18, 38 })
    void testWriteEmpty(int precision) throws Exception {
        var baos = new ByteArrayOutputStream();
        var numericType = new FbNumeric(precision, 0, RoundingMode.UNNECESSARY);
        numericType.writeEmpty(EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        getAssertions(precision).assertValue(baos.toByteArray(), BigInteger.ZERO);
    }

    @Test
    void testHashCode() {
        assertEquals(new FbNumeric(9, 2, RoundingMode.CEILING).hashCode(),
                new FbNumeric(9, 2, RoundingMode.CEILING).hashCode());
    }

    @Test
    void testEquals() {
        FbNumeric numericType = new FbNumeric(9, 2, RoundingMode.CEILING);
        assertEquals(numericType, new FbNumeric(9, 2, RoundingMode.CEILING));
        assertNotEquals(numericType, new FbNumeric(10, 2, RoundingMode.CEILING));
        assertNotEquals(numericType, new FbNumeric(9, 3, RoundingMode.CEILING));
        assertNotEquals(numericType, new FbNumeric(9, 2, RoundingMode.HALF_UP));
        Converter<BigDecimal> converter = Converter.of(BigDecimal.class, v -> BigDecimal.TEN);
        assertNotEquals(numericType, new FbNumeric(converter, 9, 2, RoundingMode.HALF_UP));
        assertEquals(new FbNumeric(converter, 9, 2, RoundingMode.HALF_UP),
                new FbNumeric(converter, 9, 2, RoundingMode.HALF_UP));
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(numericType, new  FbChar(10, FbEncoding.ASCII));
    }

    private static byte[] writeAndGetValue(FbNumeric numericType, String valueToWrite) throws IOException {
        return writeAndGetValue(numericType, valueToWrite, null);
    }

    private static byte[] writeAndGetValue(FbNumeric numericType, String valueToWrite,
            Converter<BigDecimal> converter) throws IOException {
        var baos = new ByteArrayOutputStream();
        numericType.withConverter(converter)
                .writeValue(valueToWrite, EncoderOutputStream.of(ByteOrderType.AUTO).withColumnCount(1).writeTo(baos));
        return baos.toByteArray();
    }

    private static FixedPointBackingTypeAssertions getAssertions(int precision) {
        if (precision <= 4) {
            return FixedPointBackingTypeAssertions.SHORT;
        } else if (precision <= 9) {
            return FixedPointBackingTypeAssertions.INTEGER;
        } else if (precision <= 18) {
            return FixedPointBackingTypeAssertions.BIGINT;
        } else {
            return FixedPointBackingTypeAssertions.INT128;
        }
    }

}
