// SPDX-FileCopyrightText: Copyright 2026 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import nl.lawinegevaar.exttablegen.convert.Converter;
import org.firebirdsql.decimal.Decimal64;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * A datatype representing the Firebird datatype {@code DECFLOAT(16)}.
 *
 * @since 3
 */
public final class FbDecfloat16 extends AbstractFbDecimalFloatingPointDatatype<Decimal64> {

    public FbDecfloat16(@Nullable DecfloatOnOverflow onOverflow) {
        this(null, onOverflow);
    }

    FbDecfloat16(@Nullable Converter<BigDecimal> bigDecimalConverter, @Nullable DecfloatOnOverflow onOverflow) {
        super(new Decimal64Type(onOverflow), bigDecimalConverter);
    }

    private FbDecfloat16(@Nullable Converter<Decimal64> converter, BackingType<Decimal64> backingType) {
        super(converter, backingType);
    }

    @Override
    public FbDecfloat16 withConverter(@Nullable Converter<Decimal64> converter) {
        if (hasConverter(converter)) return this;
        return new FbDecfloat16(converter, backingType());
    }

}
