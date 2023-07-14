// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class RangeChecksTest {

    @ParameterizedTest
    @ValueSource(ints = { 2, 10, 16, 32, 36 })
    void testCheckRadix_inRange(int radix) {
        assertDoesNotThrow(() -> RangeChecks.checkRadix(radix));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 37, 38})
    void testCheckRadix_outOfRange(int radix) {
        assertThrows(IllegalArgumentException.class, () -> RangeChecks.checkRadix(radix));
    }

    @ParameterizedTest
    @ValueSource(strings = { "-80000000000000000000000000000000", "7fffffffffffffffffffffffffffffff" })
    void testCheckInt128Range_inRange(String bigIntegerHex) {
        var value = new BigInteger(bigIntegerHex, 16);
        assertDoesNotThrow(() -> RangeChecks.checkInt128Range(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "-80000000000000000000000000000001", "80000000000000000000000000000000" })
    void testCheckInt128Range_outOfRange(String bigIntegerHex) {
        var value = new BigInteger(bigIntegerHex, 16);
        assertThrows(NumberFormatException.class, () -> RangeChecks.checkInt128Range(value));
    }

}