// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Abstract class for floating point number converters which parse strings using a locale.
 *
 * @param <U>
 *         target type of the converter
 * @since 3
 */
public abstract sealed class AbstractParseFloatingPointNumber<U extends Number> implements Converter<U>
        permits ParseDoublePrecision, ParseFloat {

    private final Locale locale;
    private final ThreadLocal<DecimalFormat> threadDecimalFormat;

    AbstractParseFloatingPointNumber(Locale locale) {
        this.locale = requireNonNull(locale, "locale");
        var format = NumberFormat.getInstance(locale);
        if (format instanceof DecimalFormat df) {
            // DecimalFormat is not thread safe;
            // although the current implementation is single-threaded, we want to be prepared
            threadDecimalFormat = ThreadLocal.withInitial(() -> (DecimalFormat) df.clone());
        } else {
            throw new IllegalArgumentException(
                    "Locale %s did not produce an instance of java.text.DecimalFormat as expected, but a %s"
                            .formatted(locale.toLanguageTag(), format.getClass().getName()));
        }
    }

    /**
     * @return locale of this {@code AbstractParseFloatingPointNumber} (never {@code null})
     */
    public Locale locale() {
        return locale;
    }
    
    final Number parse(String sourceValue) {
        var parsePosition = new ParsePosition(0);
        Number parsedValue = threadDecimalFormat.get().parse(sourceValue, parsePosition);
        if (parsePosition.getErrorIndex() != -1) {
            throw new NumberFormatException("Could not parse value '%s' to Number at index %d"
                    .formatted(sourceValue, parsePosition.getErrorIndex()));
        }
        int length = sourceValue.length();
        if (parsePosition.getIndex() < length) {
            throw new NumberFormatException(
                    "Could not parse value '%s' to BigDecimal, parsing ended at index %d, but value has %d characters remaining"
                            .formatted(sourceValue, parsePosition.getIndex(), length - parsePosition.getIndex()));
        }
        return parsedValue;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The converter name for all subclasses of {@link AbstractParseFloatingPointNumber} is the same:
     * {@code "parseFloatingPointNumber"}.
     * </p>
     */
    @Override
    public final String converterName() {
        return "parseFloatingPointNumber";
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
               && converter.unwrap() instanceof AbstractParseFloatingPointNumber<?> that
               && this.locale.equals(that.locale)
               && this.getClass() == that.getClass();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The hash code is derived from the class hash code.
     * </p>
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), locale);
    }

    @Override
    public final String toString() {
        return converterName() + "{locale=" + locale + '}';
    }

}
