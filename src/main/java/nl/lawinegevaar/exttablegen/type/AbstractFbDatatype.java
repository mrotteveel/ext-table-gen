// SPDX-FileCopyrightText: Copyright 2023-2024 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;

/**
 * Abstract partial implementation of {@link FbDatatype} for common code of data type implementations.
 *
 * @param <T>
 *         target type
 * @param <C>
 *         specialised {@link Converter} type
 * @since 2
 */
public abstract sealed class AbstractFbDatatype<T, C extends Converter<T>> implements FbDatatype<T>
        permits AbstractFbFixedPointDatatype, FbBigint, FbChar, FbDate, FbDoublePrecision, FbFloat, FbInt128, FbInteger,
        FbSmallint, FbTime, FbTimestamp {

    private final Class<T> targetType;
    // The configured converter (can be null)
    private final C converter;
    // The final converter (either configured or the default)
    private final C finalConverter;

    /**
     * Initializes this abstract data type.
     * <p>
     * We're separating {@code converter} and {@code defaultConverter}, so we can distinguish the explicitly configured
     * converter from the default (e.g. when generating a configuration file we don't want to include the default).
     * </p>
     *
     * @param targetType
     *         class of the target type
     * @param converter
     *         configured converter (can be {@code null})
     * @param defaultConverter
     *         default converter to use when {@code converter} is {@code null}
     */
    protected AbstractFbDatatype(Class<T> targetType, C converter, C defaultConverter) {
        this.targetType = targetType;
        this.converter = converter;
        this.finalConverter = requireNonNullElse(converter, defaultConverter);
    }

    @Override
    public final Class<T> targetType() {
        return targetType;
    }

    @Override
    public final Optional<C> converter() {
        return Optional.ofNullable(converter);
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeValueImpl(finalConverter().convert(value), out);
        }
    }

    @SuppressWarnings("java:S3038")
    @Override
    public abstract void writeEmpty(EncoderOutputStream out) throws IOException;

    /**
     * Writes value of type {@code T} to {@code out}.
     *
     * @param value
     *         value to write
     * @param out
     *         encoder output stream
     * @throws IOException
     *         for errors writing to the stream
     */
    protected abstract void writeValueImpl(T value, EncoderOutputStream out) throws IOException;

    /**
     * @return the converter to apply (is either {@link #converter()} or the default converter if empty)
     */
    protected final C finalConverter() {
        return finalConverter;
    }

    /**
     * Checks if the (non-default) converter of this data type is equal to {@code converter}.
     *
     * @param converter
     *         converter
     * @return {@code true} if the (non-default) converter of this instance is equal to {@code converter}
     */
    protected boolean hasConverter(C converter) {
        return Objects.equals(this.converter, converter);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' +
               "converter=" + converter +
               '}';
    }
}
