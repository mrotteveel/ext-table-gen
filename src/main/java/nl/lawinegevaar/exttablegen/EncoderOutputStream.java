// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import org.apache.commons.lang3.ArrayUtils;

import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 * Output stream for endian-sensitive operations.
 *
 * @since 2
 */
final class EncoderOutputStream extends FilterOutputStream {

    private static final int REQUIRED_CAPACITY = 8;
    private final ByteBuffer byteBuffer;
    private final WritableByteChannel channel;

    private EncoderOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out);
        if (out instanceof EncoderOutputStream) {
            throw new IllegalArgumentException(
                    "An EncoderOutputStream should not wrap an instance of EncoderOutputStream");
        }
        byteBuffer = out instanceof FileOutputStream
                ? ByteBuffer.allocateDirect(REQUIRED_CAPACITY)
                : ByteBuffer.allocate(REQUIRED_CAPACITY);
        byteBuffer.order(byteOrder);
        channel = Channels.newChannel(out);
    }

    void writeShort(short v) throws IOException {
        byteBuffer.clear();
        byteBuffer.putShort(v);
        writeBuffer();
    }

    void writeInt(int v) throws IOException {
        byteBuffer.clear();
        byteBuffer.putInt(v);
        writeBuffer();
    }

    void writeLong(long v) throws IOException {
        byteBuffer.clear();
        byteBuffer.putLong(v);
        writeBuffer();
    }

    void writeInt128(BigInteger v) throws IOException {
        if (v.bitLength() > 127) {
            throw new NumberFormatException("Received value requires more than 16 bytes storage: " + v);
        }
        if (v.equals(BigInteger.ZERO)) {
            out.write(new byte[16]);
            return;
        }
        byte[] bytes = v.toByteArray();
        if (bytes.length < 16) {
            byte[] int128Bytes = new byte[16];
            int startOfMinimum = 16 - bytes.length;
            if (v.signum() == -1) {
                // extend sign
                Arrays.fill(int128Bytes, 0, startOfMinimum, (byte) -1);
            }
            System.arraycopy(bytes, 0, int128Bytes, startOfMinimum, bytes.length);
            bytes = int128Bytes;
        }
        out.write(fromNetworkOrder(bytes));
    }

    private byte[] fromNetworkOrder(byte[] bytes) {
        if (byteBuffer.order() == ByteOrder.LITTLE_ENDIAN) {
            ArrayUtils.reverse(bytes);
        }
        return bytes;
    }

    private void writeBuffer() throws IOException {
        byteBuffer.flip();
        do {
            channel.write(byteBuffer);
        } while (byteBuffer.hasRemaining());
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            channel.close();
        }
    }

    static Builder of(ByteOrderType byteOrder) {
        return switch (byteOrder.effectiveValue()) {
            case BIG_ENDIAN -> out -> new EncoderOutputStream(out, ByteOrder.BIG_ENDIAN);
            case LITTLE_ENDIAN -> out -> new EncoderOutputStream(out, ByteOrder.LITTLE_ENDIAN);
            default -> throw new IllegalArgumentException("Unexpected effective value for " + byteOrder);
        };
    }

    @FunctionalInterface
    interface Builder {

        EncoderOutputStream with(OutputStream outputStream);

    }

}
