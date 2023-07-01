// SPDX-FileCopyrightText: 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Special column to add a linebreak to the end of each row.
 */
final class EndColumn extends Column {

    private static final EndColumn LF_COLUMN = new EndColumn("LF", new FbChar(1, FbEncoding.ASCII), new byte[] { '\n' });
    private static final EndColumn CRLF_COLUMN =
            new EndColumn("CRLF", new FbChar(2, FbEncoding.ASCII), new byte[] { '\r', '\n' });

    private final byte[] value;

    private EndColumn(String name, FbChar datatype, byte[] value) {
        super(name, datatype);
        this.value = value;
    }

    /**
     * Creates an end column of {@code type}.
     *
     * @param type
     *         type of end column
     * @return end column of {@code type}, or empty for {@code NONE}; may return the same instance on each invocation
     */
    static Optional<EndColumn> of(Type type) {
        return switch (type) {
            case LF -> Optional.of(LF_COLUMN);
            case CRLF -> Optional.of(CRLF_COLUMN);
            case NONE -> Optional.empty();
        };
    }

    /**
     * Creates and end column of {@code type}.
     * <p>
     * Contrary to {@link #of(Type)}, this method throws an exception for {@link Type#NONE}. This is primarily intended
     * for use by tests.
     * </p>
     *
     * @param type
     *         type of end column
     * @return end column of {@code type}
     * @throws IllegalArgumentException
     *         if {@code type} is {@code NONE}
     */
    static EndColumn require(Type type) {
        return switch (type) {
            case LF -> LF_COLUMN;
            case CRLF -> CRLF_COLUMN;
            case NONE -> throw new IllegalArgumentException("End column type NONE has no EndColumn instance");
        };
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        writeEmpty(out);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        out.write(this.value);
    }

    @Override
    void appendColumnDefinition(StringBuilder sb) {
        super.appendColumnDefinition(sb);
        sb.append(" default _ASCII x'").append(HexFormat.of().formatHex(value)).append('\'');
    }

    @Override
    public boolean equals(Object obj) {
        // Works correctly because of factory method always returning the same instances
        return this == obj;
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    /**
     * Type of end column (with linebreak) to add as last column.
     */
    enum Type {

        /**
         * No end column.
         */
        NONE,
        /**
         * Single character end column with linefeed (a.k.a. LF, 0x0A, or \n).
         */
        LF,
        /**
         * Two character end column with carriage return and linefeed (a.k.a. CRLF, 0x0D 0x0A, or \r\n).
         */
        CRLF;

        /**
         * @return end column instance for this type, or empty for {@code NONE}
         */
        Optional<EndColumn> getEndColumn() {
            return of(this);
        }

    }

}
