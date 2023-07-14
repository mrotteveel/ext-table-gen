// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

/**
 * Converter from string to short ({@code SMALLINT}).
 *
 * @since 2
 */
public final class ParseSmallint extends AbstractParseIntegralNumber<Short>
        implements ShortConverter {

    private static final ParseSmallint RADIX_TEN = new ParseSmallint(10);

    private ParseSmallint(int radix) {
        super(radix);
    }

    /**
     * Returns a {@code ParseSmallint} with the specified radix.
     *
     * @param radix
     *         desired radix (range [2, 36])
     * @return converter instance (possibly a cached instance)
     * @throws IllegalArgumentException
     *         if the radix is out of range
     */
    public static ParseSmallint ofRadix(int radix) {
        if (radix == 10) {
            return RADIX_TEN;
        }
        return new ParseSmallint(radix);
    }

    @Override
    public Class<Short> targetType() {
        return Short.class;
    }

    @Override
    public short convertToShort(String sourceValue) {
        return Short.parseShort(sourceValue, radix());
    }

}
