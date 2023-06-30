// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.FilterOutputStream;
import java.io.OutputStream;

/**
 * Output stream for endian-sensitive operations.
 *
 * @since 2
 */
abstract sealed class EncoderOutputStream extends FilterOutputStream
        permits BigEndianEncoderOutputStream, LittleEndianEncoderOutputStream {

    EncoderOutputStream(OutputStream out) {
        super(out);
        if (out instanceof EncoderOutputStream) {
            throw new IllegalArgumentException("A CoderOutputStream should not wrap an instance of CoderOutputStream");
        }
    }

    static Builder of(ByteOrderType byteOrder) {
        return switch (byteOrder.effectiveValue()) {
            case BIG_ENDIAN -> BigEndianEncoderOutputStream::new;
            case LITTLE_ENDIAN -> LittleEndianEncoderOutputStream::new;
            default -> throw new IllegalArgumentException("Unexpected effective value for " + byteOrder);
        };
    }

    @FunctionalInterface
    interface Builder {

        EncoderOutputStream with(OutputStream outputStream);

    }
}

final class BigEndianEncoderOutputStream extends EncoderOutputStream {

    BigEndianEncoderOutputStream(OutputStream out) {
        super(out);
    }

}

final class LittleEndianEncoderOutputStream extends EncoderOutputStream {

    LittleEndianEncoderOutputStream(OutputStream out) {
        super(out);
    }

}