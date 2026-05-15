// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A datatype representing a Firebird character string datatype with a specific length and character set.
 */
public abstract sealed class AbstractFbStringDataType extends AbstractFbDatatype<String, Converter<String>>
        implements FbCharacterDataType<String> permits FbChar, FbVarchar {

    private static final Converter<String> DEFAULT_CONVERTER =
            Converter.of(String.class, Function.identity());

    private final int length;
    private final FbEncoding encoding;
    private final int maxByteLength;

    /**
     * Creates a column datatype representing a Firebird character string type.
     *
     * @param length
     *         length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the column
     */
    AbstractFbStringDataType(int length, FbEncoding encoding) {
        this(length, encoding, null);
    }

    /**
     * Creates a column datatype representing a Firebird character string type.
     *
     * @param length
     *         length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the column
     * @param converter
     *         converter to apply
     */
    AbstractFbStringDataType(int length, FbEncoding encoding, @Nullable Converter<String> converter) {
        super(String.class, converter, DEFAULT_CONVERTER);
        if (length < 1) {
            throw new IllegalArgumentException("Minimum size is 1 character");
        }
        this.length = length;
        this.encoding = requireNonNull(encoding, "encoding");
        maxByteLength = encoding.maxByteLength(length);
    }

    @Override
    public final int length() {
        return length;
    }

    @Override
    public final FbEncoding encoding() {
        return encoding;
    }

    final int maxByteLength() {
        return maxByteLength;
    }

    /**
     * Gets the maximum length in Java {@code char} to use from {@code value}.
     * <p>
     * Specifically, it returns the number of Java {@code char} such that the number of codepoints is at most
     * {@code length}.
     * </p>
     *
     * @param value
     *         string value
     * @return number of Java {@code char} to use from the string to get at most {@code length} codepoints
     */
    final int getMaxLength(String value) {
        int valueLength = value.length();
        if (valueLength > length) {
            int codePoints = value.codePointCount(0, valueLength);
            if (codePoints > length) {
                return value.offsetByCodePoints(0, length);
            }
        }
        return valueLength;
    }

    @Override
    protected final void writeValueImpl(String value, EncoderOutputStream out) throws IOException {
        writeValueBytes(out, encoding().getBytes(value, getMaxLength(value)));
    }

    void writeValueBytes(EncoderOutputStream out, byte[] bytes) throws IOException {
        out.write(bytes);
        writePaddingFor(bytes.length, out);
    }

    @Override
    public final void writeEmpty(EncoderOutputStream out) throws IOException {
        writeValueBytes(out, new byte[0]);
    }

    /**
     * @return the padding char to write
     */
    abstract int padChar();

    /**
     * Writes the necessary padding for {@code length} written bytes ({@code maxByteLength - length} spaces).
     *
     * @param byteLength
     *         number of bytes written
     * @param out
     *         output stream
     * @throws IOException
     *         for errors writing to {@code out}
     */
    final void writePaddingFor(int byteLength, OutputStream out) throws IOException {
        int padSize = maxByteLength - byteLength;
        if (padSize <= 0) return;
        var bytes = new byte[padSize];
        Arrays.fill(bytes, (byte) padChar());
        out.write(bytes);
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        // Given maxByteLength is derived, we don't need to include it
        return obj instanceof AbstractFbStringDataType that
                && this.getClass() == that.getClass()
                && this.length == that.length
                && this.encoding.equals(that.encoding)
                && this.converter().equals(that.converter());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(length, encoding, converter());
    }

}
