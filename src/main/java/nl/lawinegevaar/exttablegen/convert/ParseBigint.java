// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

/**
 * Converter from string to long ({@code BIGINT}).
 *
 * @since 2
 */
public final class ParseBigint extends AbstractParseIntegralNumber<Long>
        implements LongConverter {

    private static final ParseBigint RADIX_TEN = new ParseBigint(10);

    private ParseBigint(int radix) {
        super(radix);
    }

    /**
     * Returns a {@code ParseBigint} with the specified radix.
     *
     * @param radix
     *         desired radix (range [2, 36])
     * @return converter instance (possibly a cached instance)
     * @throws IllegalArgumentException
     *         if the radix is out of range
     */
    public static ParseBigint ofRadix(int radix) {
        if (radix == 10) {
            return RADIX_TEN;
        }
        return new ParseBigint(radix);
    }

    @Override
    public Class<Long> targetType() {
        return Long.class;
    }

    @Override
    public long convertToLong(String sourceValue) {
        return Long.parseLong(sourceValue, radix());
    }

}
