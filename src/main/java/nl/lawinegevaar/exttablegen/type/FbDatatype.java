// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;

import java.io.IOException;
import java.util.Optional;

/**
 * Represents a Firebird datatype (with length, precision and/or scale).
 */
public sealed interface FbDatatype<T>
        permits AbstractFbDatatype, FbChar, FbDatetimeType, FbFixedPointDatatype, FbIntegralNumberDatatype {

    /**
     * @return Java type used by this data type
     * @since 2
     */
    Class<T> targetType();

    /**
     * @return non-default converter configured for this data type
     * @since 2
     */
    Optional<? extends Converter<T>> converter();

    /**
     * Appends the type definition of this datatype (this includes length, precision, scale, character set name, etc.).
     *
     * @param sb
     *         string builder
     */
    void appendTypeDefinition(StringBuilder sb);

    /**
     * Write {@code value}, applying {@link #converter()} or the default converter to convert to the target type.
     * <p>
     * For string type columns, if the value is longer than the column length, the truncated value must be written. For
     * other types, if the value exceeds the range of a type (e.g. assigning 2^31 or higher to an {@code INT} column),
     * an exception should be thrown (T.B.D when implementing types other the {@code CHAR}). When a value exceeds
     * the scale (e.g. assigning {@code 12.345} to a {@code NUMERIC(18,2)} the value should be truncated to
     * {@code 12.34})
     * </p>
     *
     * @param value
     *         value to write, for non-string types, the value {@code ""} or {@code null} should write an appropriate
     *         default value (e.g. {@code 0} for integer types)
     * @param out
     *         output stream to write to
     * @throws IOException
     *         for errors writing to the stream
     */
    void writeValue(String value, EncoderOutputStream out) throws IOException;

    /**
     * Writes an empty value to {@code out}.
     * <p>
     * The default implementation calls {@code writeValue("", out)}.
     * </p>
     *
     * @param out
     *         output stream to write to
     */
    default void writeEmpty(EncoderOutputStream out) throws IOException {
        writeValue("", out);
    }

    /**
     * Creates a new data type instance with the specified converter.
     * <p>
     * Though not required, implementations may return the same instance if {@code converter} is equal to the
     * current converter.
     * </p>
     *
     * @param converter
     *         converter (can be {@code null})
     * @return new data type with {@code converter}
     * @see #withConverterChecked(Converter)
     * @since 2
     */
    FbDatatype<T> withConverter(Converter<T> converter);

    /**
     * Variant of {@link #withConverter(Converter)} which checks if the target type of this data type and
     * {@code converter} are the same.
     *
     * @param converter
     *         converter (can be {@code null})
     * @return new data type with {@code converter}
     * @see #withConverter(Converter)
     * @since 2
     */
    default FbDatatype<T> withConverterChecked(Converter<?> converter) {
        return withConverter(converter != null ? converter.checkedCast(targetType()) : null);
    }

}
