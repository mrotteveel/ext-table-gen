// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.EncoderOutputStream;
import nl.lawinegevaar.exttablegen.convert.Converter;
import nl.lawinegevaar.exttablegen.convert.ParseInt128;

import java.io.IOException;
import java.math.BigInteger;

/**
 * A data type representing the Firebird datatype {@code INT128}.
 *
 * @since 2
 */
public final class FbInt128 extends AbstractFbDatatype<BigInteger, Converter<BigInteger>>
        implements FbIntegralNumberDatatype<BigInteger> {

    static final BigInteger MIN_VALUE = new BigInteger("-80000000000000000000000000000000", 16);
    static final BigInteger MAX_VALUE = new BigInteger("7fffffffffffffffffffffffffffffff", 16);

    private static final ParseInt128 DEFAULT_CONVERTER = ParseInt128.ofRadix(10);

    /**
     * Constructs a {@code FbInt128} using the default conversion.
     */
    public FbInt128() {
        this(null);
    }

    /**
     * Constructs a {@code FbInt128} with a converter.
     *
     * @param converter
     *         converter, or {@code null} for the default conversion
     */
    public FbInt128(Converter<BigInteger> converter) {
        super(BigInteger.class, converter, DEFAULT_CONVERTER);
    }

    @Override
    public void appendTypeDefinition(StringBuilder sb) {
        sb.append("int128");
    }

    @Override
    protected void writeValueImpl(BigInteger value, EncoderOutputStream out) throws IOException {
        out.align(8);
        out.writeInt128(value);
    }

    @Override
    public void writeEmpty(EncoderOutputStream out) throws IOException {
        writeValueImpl(BigInteger.ZERO, out);
    }

    @Override
    public FbInt128 withConverter(Converter<BigInteger> converter) {
        if (hasConverter(converter)) return this;
        return new FbInt128(converter);
    }

    @Override
    public int hashCode() {
        return 31 * FbInt128.class.hashCode() + converter().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof FbInt128 that
               && this.converter().equals(that.converter());
    }

}
