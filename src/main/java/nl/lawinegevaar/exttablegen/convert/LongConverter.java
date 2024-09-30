// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import org.jspecify.annotations.Nullable;

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

    static @Nullable LongConverter wrap(@Nullable Converter<Long> converter) {
        final class LongConverterAdapter extends AbstractConverterAdapter<Long> implements LongConverter {
            LongConverterAdapter(Converter<Long> converter) {
                super(converter);
            }

            @Override
            public long convertToLong(String sourceValue) {
                return convert(sourceValue);
            }
        }
        if (converter == null) return null;
        if (converter instanceof LongConverter longConverter) {
            return longConverter;
        }
        return new LongConverterAdapter(converter);
    }


}
