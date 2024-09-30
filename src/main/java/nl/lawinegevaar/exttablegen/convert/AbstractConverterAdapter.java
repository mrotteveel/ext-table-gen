// SPDX-FileCopyrightText: Copyright 2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import static java.util.Objects.requireNonNull;

/**
 * Abstract implementation for converter adapters.
 *
 * @param <T>
 *         target type of the converter
 * @since 3
 */
abstract class AbstractConverterAdapter<T> implements Converter<T> {

    private final Converter<T> converter;

    AbstractConverterAdapter(Converter<T> converter) {
        this.converter = requireNonNull(converter, "converter");
    }

    @Override
    public final T convert(String sourceValue) {
        return converter.convert(sourceValue);
    }

    @Override
    public final Class<T> targetType() {
        return converter.targetType();
    }

    @Override
    public final String converterName() {
        return converter.converterName();
    }

    @Override
    public final Converter<T> unwrap() {
        return converter;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Converter<?> that
               && this.converter.equals(that.unwrap());
    }

    @Override
    public final int hashCode() {
        return converter.hashCode();
    }

}
