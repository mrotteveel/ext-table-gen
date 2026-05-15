// SPDX-FileCopyrightText: Copyright 2023-2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.convert.Converter;
import org.jspecify.annotations.Nullable;

/**
 * A datatype representing the Firebird datatype {@code CHAR} with a specific length and character set.
 */
public final class FbChar extends AbstractFbStringDataType {

    /**
     * Creates a column datatype representing a Firebird {@code CHAR}.
     *
     * @param length
     *         length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the column
     */
    public FbChar(int length, FbEncoding encoding) {
        super(length, encoding);
    }

    /**
     * Creates a column datatype representing a Firebird {@code CHAR}.
     *
     * @param length
     *         length (in Unicode codepoints)
     * @param encoding
     *         Firebird encoding of the column
     * @param converter
     *         converter to apply
     * @since 2
     */
    public FbChar(int length, FbEncoding encoding, @Nullable Converter<String> converter) {
        super(length, encoding, converter);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("char(").append(length()).append(") character set ").append(encoding().firebirdName());
    }

    @Override
    public FbChar withConverter(@Nullable Converter<String> converter) {
        if (hasConverter(converter)) return this;
        return new FbChar(length(), encoding(), converter);
    }

    @Override
    int padChar() {
        return 0x20;
    }

    @Override
    public String toString() {
        return "Char{" +
               "length=" + length() +
               ", encoding=" + encoding() +
               '}';
    }

}
