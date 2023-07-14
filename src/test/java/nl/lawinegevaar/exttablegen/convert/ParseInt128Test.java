// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import nl.lawinegevaar.exttablegen.TargetTypeMismatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class ParseInt128Test {

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 37, 38 })
    void testOfRadix_rejectInvalidRadix(int radix) {
        assertThrows(IllegalArgumentException.class, () -> ParseInt128.ofRadix(radix));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            input, radix, expected
            7fffffffffffffffffffffffffffffff, 16, 170141183460469231731687303715884105727
            7fffffffffffffff, 16, 9223372036854775807
            9223372036854775807, 10, 9223372036854775807
            7fffffff, 16, 2147483647
            2147483647, 10, 2147483647
            7fff, 16, 32767
            32767, 10, 32767
            1000000001, 2, 513
            3o, 36, 132
            1, 10, 1
            0, 10, 0
            -1, 10, -1
            -32768, 10, -32768
            -8000, 16, -32768
            -2147483648, 10, -2147483648
            -80000000, 16, -2147483648
            -9223372036854775808, 10, -9223372036854775808
            -8000000000000000, 16, -9223372036854775808
            -80000000000000000000000000000000, 16, -170141183460469231731687303715884105728
            """)
    void testConvert_validValues(String inputString, int radix, BigInteger expectedValue) {
        assertEquals(expectedValue, ParseInt128.ofRadix(radix).convert(inputString));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            input, radix
            80000000000000000000000000000000, 16
            170141183460469231731687303715884105728, 10
            NOT_VALID, 10
            -170141183460469231731687303715884105729, 10
            -80000000000000000000000000000001, 16
            """)
    void testConvert_invalidValues_throwsNumberFormatException(String inputString, int radix) {
        assertThrows(NumberFormatException.class,
                () -> ParseInt128.ofRadix(radix).convert(inputString));
    }

    @Test
    void testCheckedCast_validType() {
        var converter = ParseInt128.ofRadix(10);
        var castResult = assertDoesNotThrow(() -> converter.checkedCast(BigInteger.class));
        assertSame(converter, castResult);
    }

    @ParameterizedTest
    @ValueSource(classes = { Long.class, Integer.class, Short.class, String.class })
    void testCheckedCast_invalidTypes(Class<?> clazz) {
        var converter = ParseInt128.ofRadix(10);
        assertThrows(TargetTypeMismatchException.class, () -> converter.checkedCast(clazz));
    }

}