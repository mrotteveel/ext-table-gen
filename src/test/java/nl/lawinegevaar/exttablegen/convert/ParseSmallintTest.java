// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import nl.lawinegevaar.exttablegen.TargetTypeMismatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseSmallintTest {

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 37, 38 })
    void testOfRadix_rejectInvalidRadix(int radix) {
        assertThrows(IllegalArgumentException.class, () -> ParseSmallint.ofRadix(radix));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            input, radix, expected
            7fff, 16, 32767
            32767, 10, 32767
            1000000001, 2, 513
            3o, 36, 132
            1, 10, 1
            0, 10, 0
            -1, 10, -1
            -32768, 10, -32768
            -8000, 16, -32768
            """)
    void testConvert_validValues(String inputString, int radix, short expectedValue) {
        assertEquals(expectedValue, ParseSmallint.ofRadix(radix).convert(inputString));
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock =
            """
            input, radix
            8000, 16
            32768, 10
            NOT_VALID, 10
            -32769, 10
            -8001, 16
            """)
    void testConvert_invalidValues_throwsNumberFormatException(String inputString, int radix) {
        assertThrows(NumberFormatException.class,
                () -> ParseSmallint.ofRadix(radix).convert(inputString));
    }

    @Test
    void testCheckedCast_validType() {
        var converter = ParseSmallint.ofRadix(10);
        var castResult = assertDoesNotThrow(() -> converter.checkedCast(Short.class));
        assertSame(converter, castResult);
    }

    @ParameterizedTest
    @ValueSource(classes = { BigInteger.class, Long.class, Integer.class, String.class })
    void testCheckedCast_invalidTypes(Class<?> clazz) {
        var converter = ParseSmallint.ofRadix(10);
        assertThrows(TargetTypeMismatchException.class, () -> converter.checkedCast(clazz));
    }

}