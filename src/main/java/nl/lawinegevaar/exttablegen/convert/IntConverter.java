// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import static java.util.Objects.requireNonNull;

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
        final class IntConverterAdapter implements IntConverter {

            private final Converter<Integer> converter;

            IntConverterAdapter(Converter<Integer> converter) {
                this.converter = requireNonNull(converter, "converter");
            }

            @Override
            public Integer convert(String sourceValue) {
                return converter.convert(sourceValue);
            }

            @Override
            public int convertToInt(String sourceValue) {
                return convert(sourceValue);
            }

            @Override
            public Class<Integer> targetType() {
                return Integer.class;
            }

            @Override
            public String converterName() {
                return converter.converterName();
            }

            @Override
            public Converter<Integer> unwrap() {
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
        if (converter instanceof IntConverter intConverter) {
            return intConverter;
        }
        return new IntConverterAdapter(converter);
    }

}
