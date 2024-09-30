// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Converter from string to double ({@code DOUBLE PRECISION}).
 * <p>
 * This converter cannot parse numbers in scientific notation ({@link DecimalFormat} requires explicit configuration
 * with a pattern to parse scientific notation), while the default converter for {@code DOUBLE PRECISION} does support
 * scientific notation.
 * </p>
 *
 * @since 3
 */
public final class ParseDoublePrecision extends AbstractParseFloatingPointNumber<Double> implements DoubleConverter {

    ParseDoublePrecision(Locale locale) {
        super(locale);
    }

    @Override
    public Class<Double> targetType() {
        return Double.class;
    }

    @Override
    public Double convert(String sourceValue) {
        Number parsedValue = parse(sourceValue);
        return parsedValue instanceof Double d ? d : parsedValue.doubleValue();
    }

    @Override
    public double convertToDouble(String sourceValue) {
        return parse(sourceValue).doubleValue();
    }

}
