// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen;

import nl.lawinegevaar.exttablegen.util.RangeChecks;
import org.apache.commons.lang3.ArrayUtils;

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
public final class EncoderOutputStream extends FilterOutputStream {

    private static final int REQUIRED_CAPACITY = 8;
    private static final int NULL_MASK_BLOCK_SIZE = 4;
    private static final int COLUMNS_PER_NULL_MASK_BLOCK = 32;

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(REQUIRED_CAPACITY);
    private final WritableByteChannel channel;
    // Size of the null mask of a row; this is used as a virtual offset for alignment purposes
    private final int nullMaskSize;
    // NOTE: This is a virtual position, it includes an offset for the NULL mask which is not included in the row data,
    // but is taken into account for alignment purposes; the alignment of the first column is also not written, but is
    // counted
    private int positionInRow;

    private EncoderOutputStream(OutputStream out, ByteOrder byteOrder, int columnCount) {
        super(out);
        if (out instanceof EncoderOutputStream) {
            throw new IllegalArgumentException(
                    "An EncoderOutputStream should not wrap an instance of EncoderOutputStream");
        }
        byteBuffer.order(byteOrder);
        channel = Channels.newChannel(out);
        // size of unwritten NULL mask (4 bytes per 32 columns)
        nullMaskSize = NULL_MASK_BLOCK_SIZE * (1 + (columnCount - 1) / COLUMNS_PER_NULL_MASK_BLOCK);
    }

    /**
     * Signals the start of a new row.
     * <p>
     * Calling this for each row is necessary for correct calculation of alignment.
     * </p>
     */
    public void startRow() {
        // offset position with unwritten NULL mask
        positionInRow = nullMaskSize;
    }

    /**
     * Makes sure the value is aligned to multiples of {@code alignment}.
     *
     * @param alignment
     *         alignment range: [1, 8], 1 means no alignment
     * @throws IOException
     *         for errors writing the alignment
     */
    public void align(int alignment) throws IOException {
        assert 1 <= alignment && alignment <= 8 : "alignment must be [1, 8], was: " + alignment;
        int requiredBytes = alignment - positionInRow % alignment;
        if (requiredBytes > 0 && requiredBytes < alignment) {
            writeAlignment(requiredBytes);
        }
    }

    private void writeAlignment(int requiredBytes) throws IOException {
        class Holder {
            private static final byte[] PADDING_BYTES = new byte[8];
        }
        if (positionInRow != nullMaskSize) {
            write(Holder.PADDING_BYTES, 0, requiredBytes);
        } else {
            // this is alignment before the first column, don't write it, but increase the virtual position in row
            positionInRow += requiredBytes;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        positionInRow += len;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        positionInRow++;
    }

    public void writeShort(short v) throws IOException {
        byteBuffer.clear();
        byteBuffer.putShort(v);
        writeBuffer();
    }

    public void writeInt(int v) throws IOException {
        byteBuffer.clear();
        byteBuffer.putInt(v);
        writeBuffer();
    }

    public void writeLong(long v) throws IOException {
        byteBuffer.clear();
        byteBuffer.putLong(v);
        writeBuffer();
    }

    public void writeInt128(BigInteger v) throws IOException {
        if (RangeChecks.checkInt128Range(v).equals(BigInteger.ZERO)) {
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
            positionInRow += channel.write(byteBuffer);
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

    public static Builder of(ByteOrderType byteOrderType) {
        return new Builder(byteOrderType.byteOrder());
    }

    public static final class Builder {

        private final ByteOrder byteOrder;
        private int columnCount;

        private Builder(ByteOrder byteOrder) {
            this.byteOrder = byteOrder;
        }

        public Builder withColumnCount(int columnCount) {
            if (columnCount <= 0) {
                throw new IllegalArgumentException("columnCount must be greater than 0, was: " + columnCount);
            }
            this.columnCount = columnCount;
            return this;
        }

        public EncoderOutputStream writeTo(OutputStream out) {
            if (columnCount == 0) {
                throw new IllegalStateException("withColumnCount must be called first");
            }
            return new EncoderOutputStream(out, byteOrder, columnCount);
        }

    }

}
