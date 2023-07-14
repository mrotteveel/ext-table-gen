// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.convert;

import nl.lawinegevaar.exttablegen.util.RangeChecks;

import java.math.BigInteger;

/**
 * Converter from string to {@code BigInteger} ({@code INT128}), enforcing the limits of {@code INT128}.
 *
 * @since 2
 */
public final class ParseInt128 extends AbstractParseIntegralNumber<BigInteger> {

    private static final ParseInt128 RADIX_TEN = new ParseInt128(10);

    private ParseInt128(int radix) {
        super(radix);
    }

    public static ParseInt128 ofRadix(int radix) {
        if (radix == 10) {
            return RADIX_TEN;
        }
        return new ParseInt128(radix);
    }

    @Override
    public BigInteger convert(String sourceValue) {
        return RangeChecks.checkInt128Range(new BigInteger(sourceValue, radix()));
    }

    @Override
    public Class<BigInteger> targetType() {
        return BigInteger.class;
    }

}
