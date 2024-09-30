// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Converter from string to float ({@code FLOAT} ({@code REAL})).
 * <p>
 * This converter cannot parse numbers in scientific notation ({@link DecimalFormat} requires explicit configuration
 * with a pattern to parse scientific notation), while the default converter for {@code FLOAT} does support
 * scientific notation.
 * </p>
 *
 * @since 3
 */
public final class ParseFloat extends AbstractParseFloatingPointNumber<Float> implements FloatConverter {

    ParseFloat(Locale locale) {
        super(locale);
    }

    @Override
    public Class<Float> targetType() {
        return Float.class;
    }

    @Override
    public float convertToFloat(String sourceValue) {
        return parse(sourceValue).floatValue();
    }

}
