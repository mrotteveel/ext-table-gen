// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.convert.Converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A data type representing the Firebird datatype {@code CHAR} with a specific length and character set.
 */
final class FbChar extends AbstractFbDatatype<String, Converter<String>> implements FbDatatype<String> {

    private static final Converter<String> DEFAULT_CONVERTER =
            Converter.of(String.class, Function.identity());

    private final int length;
    private final FbEncoding encoding;
    private final int maxByteLength;

    /**
     * Creates a column datatype representing a Firebird {@code CHAR}.
     *
     * @param length
     *         CHAR length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the CHAR column
     */
    FbChar(int length, FbEncoding encoding) {
        this(length, encoding, null);
    }

    /**
     * Creates a column datatype representing a Firebird {@code CHAR}.
     *
     * @param length
     *         CHAR length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the CHAR column
     * @param converter
     *         converter to apply
     * @since 2
     */
    FbChar(int length, FbEncoding encoding, Converter<String> converter) {
        super(String.class, converter, DEFAULT_CONVERTER);
        if (length < 1) {
            throw new IllegalArgumentException("Minimum size is 1 character");
        }
        this.length = length;
        this.encoding = requireNonNull(encoding, "encoding");
        maxByteLength = encoding.maxByteLength(length);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("char(").append(length).append(") character set ").append(encoding.firebirdName());
    }

    @Override
    public FbChar withConverter(Converter<String> converter) {
        if (hasConverter(converter)) return this;
        return new FbChar(length, encoding, converter);
    }

    /**
     * @return {@code CHAR} length (in Unicode codepoints)
     */
    int length() {
        return length;
    }

    FbEncoding encoding() {
        return encoding;
    }

    @Override
    protected void writeValueImpl(String value, EncoderOutputStream out) throws IOException {
        byte[] bytes = encoding.getBytes(value, getMaxLength(value));
        out.write(bytes);
        writePaddingFor(bytes.length, out);
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
    private int getMaxLength(String value) {
        int valueLength = value.length();
        if (valueLength > length) {
            int codePoints = value.codePointCount(0, valueLength);
            if (codePoints > length) {
                return value.offsetByCodePoints(0, length);
            }
        }
        return valueLength;
    }

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
    private void writePaddingFor(int byteLength, OutputStream out) throws IOException {
        if (byteLength >= maxByteLength) return;
        int padSize = maxByteLength - byteLength;
        if (padSize == 1) {
            out.write(0x20);
        } else {
            var bytes = new byte[padSize];
            Arrays.fill(bytes, (byte) 0x20);
            out.write(bytes);
        }
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writePaddingFor(0, out);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        // Given maxByteLength is derived, we don't need to include it
        return obj instanceof FbChar that
               && this.length == that.length
               && this.encoding.equals(that.encoding)
               && this.converter().equals(that.converter());
    }

    @Override
    public int hashCode() {
        return 31 * (31 * length + encoding.hashCode()) + converter().hashCode();
    }

    @Override
    public String toString() {
        return "Char{" +
               "length=" + length +
               ", encoding=" + encoding +
               '}';
    }

}
