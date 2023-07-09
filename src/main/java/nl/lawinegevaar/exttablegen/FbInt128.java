// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HexFormat;

final class FbInt128 implements FbIntegralNumberDatatype {

    static final BigInteger MIN_VALUE = new BigInteger(HexFormat.of().parseHex("80000000000000000000000000000000"));
    static final BigInteger MAX_VALUE = new BigInteger("7fffffffffffffffffffffffffffffff", 16);

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("int128");
    }

    @Override
    public void writeValue(String value, EncoderOutputStream out) throws IOException {
        if (value == null || value.isEmpty()) {
            writeEmpty(out);
        } else {
            // NOTE: Range check is performed by writeInt128 in EncoderOutputStream, we're not repeating it here
            writeInt128(new BigInteger(value), out);
        }
    }

    private void writeInt128(BigInteger value, EncoderOutputStream out) throws IOException {
        out.align(8);
        out.writeInt128(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeInt128(BigInteger.ZERO, out);
    }

    @Override
    public int hashCode() {
        return FbInt128.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FbInt128;
    }
}
