// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * Output stream for endian-sensitive operations.
 *
 * @since 2
 */
final class EncoderOutputStream extends FilterOutputStream {

    private static final int REQUIRED_CAPACITY = 2;
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
