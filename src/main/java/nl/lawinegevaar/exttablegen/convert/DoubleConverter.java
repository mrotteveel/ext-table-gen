// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

/**
 * A converter to {@link Double} with an additional method for conversion to the primitive {@code double}.
 *
 * @since 3
 */
public interface DoubleConverter extends Converter<Double> {

    /**
     * Converts {@code sourceValue} to a value of type {@code double}.
     *
     * @param sourceValue
     *         source value (never {@code null} or empty string)
     * @return target value
     * @throws NumberFormatException
     *         for invalid values
     */
    double convertToDouble(String sourceValue);

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation calls {@link #convertToDouble(String)}.
     * </p>
     */
    @Override
    default Double convert(String sourceValue) {
        return convertToDouble(sourceValue);
    }

    static DoubleConverter wrap(Converter<Double> converter) {
        final class DoubleConverterAdapter extends AbstractConverterAdapter<Double> implements DoubleConverter {
            DoubleConverterAdapter(Converter<Double> converter) {
                super(converter);
            }

            @Override
            public double convertToDouble(String sourceValue) {
                return convert(sourceValue);
            }
        }
        if (converter == null) return null;
        if (converter instanceof DoubleConverter doubleConverter) {
            return doubleConverter;
        }
        return new DoubleConverterAdapter(converter);
    }
    
}
