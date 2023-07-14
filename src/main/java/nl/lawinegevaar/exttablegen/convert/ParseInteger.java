// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

/**
 * Converter from string to int ({@code INTEGER}).
 *
 * @since 2
 */
public final class ParseInteger extends AbstractParseIntegralNumber<Integer>
        implements IntConverter {

    private static final ParseInteger RADIX_TEN = new ParseInteger(10);

    private ParseInteger(int radix) {
        super(radix);
    }

    /**
     * Returns a {@code ParseInteger} with the specified radix.
     *
     * @param radix
     *         desired radix (range [2, 36])
     * @return converter instance (possibly a cached instance)
     * @throws IllegalArgumentException
     *         if the radix is out of range
     */
    public static ParseInteger ofRadix(int radix) {
        if (radix == 10) {
            return RADIX_TEN;
        }
        return new ParseInteger(radix);
    }

    @Override
    public Class<Integer> targetType() {
        return Integer.class;
    }

    @Override
    public int convertToInt(String sourceValue) {
        return Integer.parseInt(sourceValue, radix());
    }

}
