// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import static java.util.Objects.requireNonNull;

/**
 * A converter to {@link Long} with an additional method for conversion to the primitive {@code long}.
 *
 * @since 2
 */
public interface LongConverter extends Converter<Long> {

    /**
     * Converts {@code sourceValue} to a value of type {@code long}.
     *
     * @param sourceValue
     *         source value (never {@code null} or empty string)
     * @return target value
     * @throws NumberFormatException
     *         for invalid values
     */
    long convertToLong(String sourceValue);

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation calls {@link #convertToLong(String)}.
     * </p>
     */
    @Override
    default Long convert(String sourceValue) {
        return convertToLong(sourceValue);
    }

    static LongConverter wrap(Converter<Long> converter) {
        final class LongConverterAdapter implements LongConverter {

            private final Converter<Long> converter;

            public LongConverterAdapter(Converter<Long> converter) {
                this.converter = requireNonNull(converter, "converter");
            }

            @Override
            public Long convert(String sourceValue) {
                return converter.convert(sourceValue);
            }

            @Override
            public long convertToLong(String sourceValue) {
                return convert(sourceValue);
            }

            @Override
            public Class<Long> targetType() {
                return Long.class;
            }

            @Override
            public String converterName() {
                return converter.converterName();
            }

            @Override
            public Converter<Long> unwrap() {
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
        if (converter instanceof LongConverter longConverter) {
            return longConverter;
        }
        return new LongConverterAdapter(converter);
    }


}
