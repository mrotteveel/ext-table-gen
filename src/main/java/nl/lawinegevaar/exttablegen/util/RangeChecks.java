// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.util;

import java.math.BigInteger;

/**
 * Helper class for various range checks.
 *
 * @since 2
 */
public final class RangeChecks {

    private RangeChecks() {
        // no instances
    }

    /**
     * Checks if {@code value} is in range of an {@code INT128}.
     *
     * @param value
     *         value to check
     * @return {@code value} if within range
     * @throws NumberFormatException
     *         if {@code value} is too big to fit in an {@code INT128}
     */
    public static BigInteger checkInt128Range(BigInteger value) {
        if (value.bitLength() > 127) {
            throw new NumberFormatException(
                    "Value is out of range for an INT128 (requires more than 16 bytes): " + value);
        }
        return value;
    }

    /**
     * Checks if {@code radix} is a valid radix (in range [2, 36]).
     *
     * @param radix
     *         radix
     * @return {@code radix} if within range
     * @throws IllegalArgumentException
     *         if {@code radix} is not in range [2, 36]
     */
    public static int checkRadix(int radix) {
        if (Character.MIN_RADIX <= radix && radix <= Character.MAX_RADIX) {
            return radix;
        }
        throw new IllegalArgumentException("radix must be in [2, 36], was: " + radix);
    }

}
