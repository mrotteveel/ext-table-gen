// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public enum FixedPointBackingTypeAssertions {

    SHORT {
        @Override
        void assertValue(byte[] rawData, BigInteger unscaledValue) {
            assertEquals(unscaledValue.shortValueExact(), wrap(rawData).getShort());
        }
    },
    INTEGER {
        @Override
        void assertValue(byte[] rawData, BigInteger unscaledValue) {
            assertEquals(unscaledValue.intValueExact(), wrap(rawData).getInt());
        }
    },
    BIGINT {
        @Override
        void assertValue(byte[] rawData, BigInteger unscaledValue) {
            assertEquals(unscaledValue.longValueExact(), wrap(rawData).getLong());
        }
    },
    INT128 {
        @Override
        void assertValue(byte[] rawData, BigInteger unscaledValue) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                byte[] bytes = rawData.clone();
                ArrayUtils.reverse(bytes);
                rawData = bytes;
            }
            var dataVal = new BigInteger(rawData);
            assertEquals(unscaledValue, dataVal);
        }
    };

    abstract void assertValue(byte[] rawData, BigInteger unscaledValue);

    ByteBuffer wrap(byte[] rawData) {
        var buf = ByteBuffer.wrap(rawData);
        buf.order(ByteOrder.nativeOrder());
        return buf;
    }

}
