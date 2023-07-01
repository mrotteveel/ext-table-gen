// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;

/**
 * Represents a Firebird datatype (with length, precision and/or scale).
 */
sealed interface FbDatatype permits FbChar, FbIntegralNumberDatatype {

    /**
     * Appends the type definition of this datatype (this includes length, precision, scale, character set name, etc.).
     *
     * @param sb
     *         string builder
     */
    void appendTypeDefinition(StringBuilder sb);

    /**
     * Write {@code value}.
     * <p>
     * For string type columns, if the value is longer than the column length, the truncated value must be written. For
     * other types, if the value exceeds the range of a type (e.g. assigning 2^31 or higher to an {@code INT} column),
     * an exception should be thrown (T.B.D when implementing types other the {@code CHAR}). When a value exceeds
     * the scale (e.g. assigning {@code 12.345} to a {@code NUMERIC(18,2)} the value should be truncated to
     * {@code 12.34})
     * </p>
     *
     * @param value
     *         value to write, for non-string types, the value {@code ""} should write an appropriate default value
     *         (e.g. {@code 0} for integer types).
     * @param out
     *         output stream to write to
     */
    // TODO We may need a generic writeValue(T, OutputStream) if we're going to support other types, or at least
    //  determine how to handle the conversion from CSV string input (taking into account thinks like date formats, etc)
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
}
