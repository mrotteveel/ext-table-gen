// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
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
        final class ShortConverterAdapter implements ShortConverter {

            private final Converter<Short> converter;

            public ShortConverterAdapter(Converter<Short> converter) {
                this.converter = converter;
            }

            @Override
            public Short convert(String sourceValue) {
                return converter.convert(sourceValue);
            }

            @Override
            public short convertToShort(String sourceValue) {
                return convert(sourceValue);
            }

            @Override
            public Class<Short> targetType() {
                return Short.class;
            }

            @Override
            public String converterName() {
                return converter.converterName();
            }

            @Override
            public Converter<Short> unwrap() {
                return converter;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                return obj instanceof Converter<?> that
                       && this.converter.equals(that.unwrap());
            }

            @Override
            public int hashCode() {
                return converter.hashCode();
            }
            
        }
        if (converter == null) return null;
        if (converter instanceof ShortConverter shortConverter) {
            return shortConverter;
        }
        return new ShortConverterAdapter(converter);
    }

}
