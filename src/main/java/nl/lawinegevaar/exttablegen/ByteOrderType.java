// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.nio.ByteOrder;

/**
 * Byte order used for writing values.
 *
 * @since 2
 */
enum ByteOrderType {

    /**
     * Big-endian.
     */
    BIG_ENDIAN(ByteOrder.BIG_ENDIAN),
    /**
     * Little-endian
     */
    LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN),
    /**
     * Select automatically based on the runtime value of {@link ByteOrder#nativeOrder()}.
     */
    AUTO(ByteOrder.nativeOrder()) {
        @Override
        public ByteOrderType effectiveValue() {
            return valueOf(byteOrder());
        }
    };

    private final ByteOrder byteOrder;

    ByteOrderType(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public final ByteOrder byteOrder() {
        return byteOrder;
    }

    /**
     * Resolves the effective value of this type.
     *
     * @return for {@code AUTO}, the result of {@code valueOf(byteOrder())} (e.g. {@code LITTLE_ENDIAN}), for
     * {@code LITTLE_ENDIAN} or {@code BIG_ENDIAN}, the instance itself.
     */
    public ByteOrderType effectiveValue() {
        return this;
    }

    public static ByteOrderType valueOf(ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return LITTLE_ENDIAN;
        } else if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return BIG_ENDIAN;
        } else {
            throw new IllegalArgumentException("Unexpected value of byteOrder: " + byteOrder);
        }
    }
}
