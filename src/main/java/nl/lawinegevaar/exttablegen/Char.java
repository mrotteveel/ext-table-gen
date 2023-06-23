// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/**
 * A datatype representing the Firebird datatype {@code CHAR} with a specific length and character set.
 */
final class Char implements Datatype {

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
    Char(int length, FbEncoding encoding) {
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
    public void writeValue(String value, OutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            byte[] bytes = encoding.getBytes(value, 0, getMaxLength(value));
            out.write(bytes);
            writePaddingFor(bytes.length, out);
        }
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
        if (byteLength < maxByteLength) {
            var bytes = new byte[maxByteLength - byteLength];
            Arrays.fill(bytes, (byte) 0x20);
            out.write(bytes);
        }
    }

    @Override
    public void writeEmpty(OutputStream out) throws IOException {
        writePaddingFor(0, out);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Char that)) return false;
        // Given maxByteLength is derived, we don't need to include it
        return length == that.length && encoding.equals(that.encoding);
    }

    @Override
    public int hashCode() {
        return 31 * length + encoding.hashCode();
    }

    @Override
    public String toString() {
        return "Char{" +
               "length=" + length +
               ", encoding=" + encoding +
               '}';
    }

}
