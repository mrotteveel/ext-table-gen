// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;

/**
 * A datatype representing the Firebird datatype {@code INTEGER}.
 *
 * @since 2
 */
final class FbInteger implements FbIntegralNumberDatatype {

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("integer");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeInt(Integer.parseInt(value), out);
        }
    }

    private void writeInt(int value, EncoderOutputStream out) throws IOException {
        out.align(4);
        out.writeInt(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeInt(0, out);
    }

    @Override
    public int hashCode() {
        return FbInteger.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FbInteger;
    }
}
