// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;

/**
 * A datatype representing the Firebird datatype {@code SMALLINT}.
 *
 * @since 2
 */
final class FbSmallint implements FbIntegralNumberDatatype {

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("smallint");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeShort(Short.parseShort(value), out);
        }
    }

    private void writeShort(short value, EncoderOutputStream out) throws IOException {
        out.align(2);
        out.writeShort(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeShort((short) 0, out);
    }

    @Override
    public int hashCode() {
        return FbSmallint.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FbSmallint;
    }
    
}
