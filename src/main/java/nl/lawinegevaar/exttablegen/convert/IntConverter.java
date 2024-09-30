// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

/**
 * A converter to {@link Integer} with an additional method for conversion to the primitive {@code int}.
 *
 * @since 2
 */
public interface IntConverter extends Converter<Integer> {

    /**
     * Converts {@code sourceValue} to a value of type {@code int}.
     *
     * @param sourceValue
     *         source value (never {@code null} or empty string)
     * @return target value
     * @throws NumberFormatException
     *         for invalid values
     */
    int convertToInt(String sourceValue);

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation calls {@link #convertToInt(String)}.
     * </p>
     */
    @Override
    default Integer convert(String sourceValue) {
        return convertToInt(sourceValue);
    }

    static IntConverter wrap(Converter<Integer> converter) {
        final class IntConverterAdapter extends AbstractConverterAdapter<Integer> implements IntConverter {
            IntConverterAdapter(Converter<Integer> converter) {
                super(converter);
            }

            @Override
            public int convertToInt(String sourceValue) {
                return convert(sourceValue);
            }
        }
        if (converter == null) return null;
        if (converter instanceof IntConverter intConverter) {
            return intConverter;
        }
        return new IntConverterAdapter(converter);
    }

}
