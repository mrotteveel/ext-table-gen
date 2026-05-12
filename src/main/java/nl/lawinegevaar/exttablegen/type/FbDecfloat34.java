// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.convert.Converter;
import org.firebirdsql.decimal.Decimal128;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * A datatype representing the Firebird datatype {@code DECFLOAT(16)}.
 *
 * @since 3
 */
public final class FbDecfloat34 extends AbstractFbDecimalFloatingPointDatatype<Decimal128> {

    public FbDecfloat34(@Nullable DecfloatOnOverflow onOverflow) {
        this(null, onOverflow);
    }

    FbDecfloat34(@Nullable Converter<BigDecimal> bigDecimalConverter, @Nullable DecfloatOnOverflow onOverflow) {
        super(new Decimal128Type(onOverflow), bigDecimalConverter);
    }

    private FbDecfloat34(@Nullable Converter<Decimal128> converter, BackingType<Decimal128> backingType) {
        super(converter, backingType);
    }

    @Override
    public FbDecfloat34 withConverter(@Nullable Converter<Decimal128> converter) {
        if (hasConverter(converter)) return this;
        return new FbDecfloat34(converter, backingType());
    }

}
