// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Converter from string to {@link BigDecimal}.
 * <p>
 * This converter cannot parse numbers in scientific notation ({@link DecimalFormat} requires explicit configuration
 * with a pattern to parse scientific notation), while the default converter for {@code NUMERIC} and {@code DECIMAL}
 * does support scientific notation.
 * </p>
 *
 * @since 2
 */
public final class ParseBigDecimal implements Converter<BigDecimal> {

    private final Locale locale;
    private final ThreadLocal<DecimalFormat> threadDecimalFormat;

    ParseBigDecimal(Locale locale) {
        this.locale = requireNonNull(locale, "locale");
        var format = NumberFormat.getInstance(locale);
        if (format instanceof DecimalFormat df) {
            df.setParseBigDecimal(true);
            // DecimalFormat is not thread safe;
            // although the current implementation is single-threaded, we want to be prepared
            threadDecimalFormat = ThreadLocal.withInitial(() -> (DecimalFormat) df.clone());
        } else {
            throw new IllegalArgumentException(
                    "Locale %s did not produce an instance of java.text.DecimalFormat as expected, but a %s"
                            .formatted(locale.toLanguageTag(), format.getClass().getName()));
        }
    }

    @Override
    public BigDecimal convert(String sourceValue) {
        var parsePosition = new ParsePosition(0);
        BigDecimal parsedValue = (BigDecimal) threadDecimalFormat.get().parse(sourceValue, parsePosition);
        if (parsePosition.getErrorIndex() != -1) {
            throw new NumberFormatException("Could not parse value '%s' to BigDecimal at index %d"
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
     * @return locale of this {@code ParseDatetime} (never {@code null})
     */
    public Locale locale() {
        return locale;
    }

    @Override
    public Class<BigDecimal> targetType() {
        return BigDecimal.class;
    }

    @Override
    public String converterName() {
        return "parseBigDecimal";
    }

    @Override
    public int hashCode() {
        return locale.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof ParseBigDecimal that
               && this.locale.equals(that.locale);
    }

}
