// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

/**
 * A converter to {@link Short} with an additional method for conversion to the primitive {@code short}.
 *
 * @since 2
 */
public interface ShortConverter extends Converter<Short> {

    /**
     * Converts {@code sourceValue} to a value of type {@code short}.
     *
     * @param sourceValue
     *         source value (never {@code null} or empty string)
     * @return target value
     * @throws NumberFormatException
     *         for invalid values
     */
    short convertToShort(String sourceValue);

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation calls {@link #convertToShort(String)}.
     * </p>
     */
    @Override
    default Short convert(String sourceValue) {
        return convertToShort(sourceValue);
    }

    static ShortConverter wrap(Converter<Short> converter) {
        final class ShortConverterAdapter extends AbstractConverterAdapter<Short> implements ShortConverter {
            ShortConverterAdapter(Converter<Short> converter) {
                super(converter);
            }

            @Override
            public short convertToShort(String sourceValue) {
                return convert(sourceValue);
            }
        }
        if (converter == null) return null;
        if (converter instanceof ShortConverter shortConverter) {
            return shortConverter;
        }
        return new ShortConverterAdapter(converter);
    }

}
