// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import nl.lawinegevaar.exttablegen.util.RangeChecks;

/**
 * Abstract class for integral number converters which parse strings using a radix.
 *
 * @param <U>
 *         target type of the converter
 * @since 2
 */
public abstract sealed class AbstractParseIntegralNumber<U extends Number> implements Converter<U>
        permits ParseBigint, ParseInt128,
        ParseInteger, ParseSmallint {

    private final int radix;

    /**
     * Constructor.
     *
     * @param radix
     *         radix, must be in range [2, 36]
     * @throws IllegalArgumentException
     *         if radix is out of range [2, 36]
     */
    AbstractParseIntegralNumber(int radix) {
        this.radix = RangeChecks.checkRadix(radix);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The converter name for all subclasses of {@link AbstractParseIntegralNumber} is the same:
     * {@code "parseIntegralNumber"}.
     * </p>
     */
    @Override
    public final String converterName() {
        return "parseIntegralNumber";
    }

    /**
     * @return radix applied by this converter
     */
    public final int radix() {
        return radix;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method considers instances of the same exact class with the same radix equal.
     * </p>
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Converter<?> converter
               && converter.unwrap() instanceof AbstractParseIntegralNumber<?> that
               && this.radix == that.radix
               && this.getClass() == that.getClass();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The hash code is derived from the class hash code and the radix.
     * </p>
     */
    @Override
    public final int hashCode() {
        return 31 * getClass().hashCode() + radix;
    }

    @Override
    public final String toString() {
        return converterName() + "{radix=" + radix + '}';
    }

}
