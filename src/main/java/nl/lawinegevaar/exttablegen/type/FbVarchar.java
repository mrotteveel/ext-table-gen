// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * A datatype representing the Firebird datatype {@code VARCHAR} with a specific length and character set.
 *
 * @since 3
 */
public final class FbVarchar extends AbstractFbStringDataType {

    /**
     * Creates a column datatype representing a Firebird {@code VARCHAR}.
     *
     * @param length
     *         length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the column
     */
    public FbVarchar(int length, FbEncoding encoding) {
        super(length, encoding);
    }

    /**
     * Creates a column datatype representing a Firebird {@code VARCHAR}.
     *
     * @param length
     *         length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the column
     * @param converter
     *         converter to apply
     */
    public FbVarchar(int length, FbEncoding encoding, @Nullable Converter<String> converter) {
        super(length, encoding, converter);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("varchar(").append(length()).append(") character set ").append(encoding().firebirdName());
    }

    @Override
    public FbVarchar withConverter(@Nullable Converter<String> converter) {
        if (hasConverter(converter)) return this;
        return new FbVarchar(length(), encoding(), converter);
    }

    @Override
    void writeValueBytes(EncoderOutputStream out, byte[] bytes) throws IOException {
        out.align(2);
        out.writeShort((short) bytes.length);
        super.writeValueBytes(out, bytes);
    }

    @Override
    int padChar() {
        return 0x00;
    }

    @Override
    public String toString() {
        return "Varchar{" +
               "length=" + length() +
               ", encoding=" + encoding() +
               '}';
    }

}
