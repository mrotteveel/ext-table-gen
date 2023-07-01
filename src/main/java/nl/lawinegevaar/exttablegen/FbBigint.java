// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;

final class FbBigint implements FbIntegralNumberDatatype{

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("bigint");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            writeLong(Long.parseLong(value), out);
        }
    }

    private void writeLong(long value, EncoderOutputStream out) throws IOException {
        out.writeLong(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeLong(0, out);
    }

    @Override
    public int hashCode() {
        return FbBigint.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FbBigint;
    }
    
}
