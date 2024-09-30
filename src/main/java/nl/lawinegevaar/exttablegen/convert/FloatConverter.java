// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import org.jspecify.annotations.Nullable;

/**
 * A converter to {@link Float} with an additional method for conversion to the primitive {@code float}.
 *
 * @since 3
 */
public interface FloatConverter extends Converter<Float> {

    /**
     * Converts {@code sourceValue} to a value of type {@code float}.
     *
     * @param sourceValue
     *         source value (never {@code null} or empty string)
     * @return target value
     * @throws NumberFormatException
     *         for invalid values
     */
    float convertToFloat(String sourceValue);

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation calls {@link #convertToFloat(String)}.
     * </p>
     */
    @Override
    default Float convert(String sourceValue) {
        return convertToFloat(sourceValue);
    }

    static @Nullable FloatConverter wrap(@Nullable Converter<Float> converter) {
        final class FloatConverterAdapter extends AbstractConverterAdapter<Float> implements FloatConverter {
            FloatConverterAdapter(Converter<Float> converter) {
                super(converter);
            }

            @Override
            public float convertToFloat(String sourceValue) {
                return convert(sourceValue);
            }
        }
        if (converter == null) return null;
        if (converter instanceof FloatConverter floatConverter) {
            return floatConverter;
        }
        return new FloatConverterAdapter(converter);
    }

}
