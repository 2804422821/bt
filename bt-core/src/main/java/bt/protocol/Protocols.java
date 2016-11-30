package bt.protocol;

import bt.BtException;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Provides utility functions for binary protocol implementations.
 *
 * @since 1.0
 */
public class Protocols {

    /**
     * BitTorrent message prefix size in bytes.
     *
     * @since 1.0
     */
    public static final int MESSAGE_LENGTH_PREFIX_SIZE = 4;

    /**
     * BitTorrent message ID size in bytes.
     *
     * @since 1.0
     */
    public static final int MESSAGE_TYPE_SIZE = 1;

    /**
     * BitTorrent message prefix size in bytes.
     * Message prefix is a concatenation of message length prefix and message ID.
     *
     * @since 1.0
     */
    public static final int MESSAGE_PREFIX_SIZE = MESSAGE_LENGTH_PREFIX_SIZE + MESSAGE_TYPE_SIZE;

    //-------------------------//
    //--- utility functions ---//
    //-------------------------//

    /**
     * Get 8-bytes binary representation of a {@link Long}.
     *
     * @since 1.0
     */
    public static byte[] getLongBytes(long l) {
        return new byte[] {
                (byte) (l >> 56),
                (byte) (l >> 48),
                (byte) (l >> 40),
                (byte) (l >> 32),
                (byte) (l >> 24),
                (byte) (l >> 16),
                (byte) (l >> 8),
                (byte) l};
    }

    /**
     * Get 4-bytes binary representation of an {@link Integer}.
     *
     * @since 1.0
     */
    public static byte[] getIntBytes(int i) {
        return new byte[] {
                (byte) (i >> 24),
                (byte) (i >> 16),
                (byte) (i >> 8),
                (byte) i};
    }

    /**
     * Get 2-bytes binary representation of a {@link Short}.
     *
     * @since 1.0
     */
    public static byte[] getShortBytes(int s) {
        return new byte[] {
                (byte) (s >> 8),
                (byte) s};
    }

    /**
     * Decode a binary long representation into a {@link Long}.
     *
     * @param bytes Arbitrary byte array.
     *              It's length must be at least <b>offset</b> + 8.
     * @param offset Offset in byte array to start decoding from (inclusive, 0-based)
     * @since 1.0
     */
    public static long readLong(byte[] bytes, int offset) {

        if (bytes.length < offset + Long.BYTES) {
            throw new ArrayIndexOutOfBoundsException("insufficient byte array length (length: " + bytes.length +
                    ", offset: " + offset + ")");
        }
        return ((bytes[offset]     & 0xFFL) << 56) |
               ((bytes[offset + 1] & 0xFFL) << 48) |
               ((bytes[offset + 2] & 0xFFL) << 40) |
               ((bytes[offset + 3] & 0xFFL) << 32) |
               ((bytes[offset + 4] & 0xFFL)  << 24) |
               ((bytes[offset + 5] & 0xFF)  << 16) |
               ((bytes[offset + 6] & 0xFF)  << 8)  |
                (bytes[offset + 7] & 0xFF);
    }

    /**
     * Decode a binary integer representation into an {@link Integer}.
     *
     * @param bytes Arbitrary byte array.
     *              It's length must be at least <b>offset</b> + 4.
     * @param offset Offset in byte array to start decoding from (inclusive, 0-based)
     * @since 1.0
     */
    public static int readInt(byte[] bytes, int offset) {

        if (bytes.length < offset + Integer.BYTES) {
            throw new ArrayIndexOutOfBoundsException("insufficient byte array length (length: " + bytes.length +
                    ", offset: " + offset + ")");
        }
        return ((bytes[offset]     & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8)  |
                (bytes[offset + 3] & 0xFF);
    }

    /**
     * Decode a binary short representation into a {@link Short}.
     *
     * @param bytes Arbitrary byte array.
     *              It's length must be at least <b>offset</b> + 2.
     * @param offset Offset in byte array to start decoding from (inclusive, 0-based)
     * @since 1.0
     */
    public static short readShort(byte[] bytes, int offset) {

        if (bytes.length < offset + Short.BYTES) {
            throw new ArrayIndexOutOfBoundsException("insufficient byte array length (length: " + bytes.length +
                    ", offset: " + offset + ")");
        }
        return (short)(((bytes[offset]     & 0xFF) << 8) |
                       ((bytes[offset + 1] & 0xFF)));
    }

    /**
     * Decode a binary integer representation from a buffer into an {@link Integer}.
     *
     * @param buffer Buffer to read from.
     *               Decoding will be done starting with the index denoted by {@link Buffer#position()}
     * @return Decoded integer, or null if there are insufficient bytes in buffer
     *         (i.e. <b>buffer.remaining()</b> &lt; 4)
     * @since 1.0
     */
    public static Integer readInt(ByteBuffer buffer) {
        if (buffer.remaining() < Integer.BYTES) {
            return null;
        }
        return buffer.getInt();
    }

    /**
     * Decode a binary short integer representation from a buffer into a {@link Short}.
     *
     * @param buffer Buffer to read from.
     *               Decoding will be done starting with the index denoted by {@link Buffer#position()}
     * @return Decoded short integer, or null if there are insufficient bytes in buffer
     *         (i.e. <b>buffer.remaining()</b> &lt; 2)
     * @since 1.0
     */
    public static Integer readShort(ByteBuffer buffer) {

        if (buffer.remaining() < Integer.BYTES) {
            return null;
        }
        // TODO: switch to ByteBuffer.getShort
        return ((buffer.get() << 8) & 0xFF00) + (buffer.get() & 0x00FF);
    }

    /**
     * Convenience method to check if actual message length is the same as expected length.
     *
     * @throws InvalidMessageException if <b>expectedLength</b> != <b>actualLength</b>
     * @since 1.0
     */
    public static void verifyPayloadHasLength(Class<? extends Message> type, int expectedLength, int actualLength) {
        if (expectedLength != actualLength) {
            throw new InvalidMessageException("Unexpected payload length for " + type.getSimpleName() + ": " + actualLength +
                    " (expected " + expectedLength + ")");
        }
    }

    /**
     * Sets i-th bit in a bitmask.
     *
     * @param bytes Bitmask.
     * @param i Bit index (0-based)
     * @since 1.0
     */
    public static void setBit(byte[] bytes, int i) {

        int byteIndex = (int) (i / 8d);
        if (byteIndex >= bytes.length) {
            throw new BtException("bit index is too large: " + i);
        }

        int bitIndex = i % 8;
        int shift = (7 - bitIndex);
        int bitMask = 0b1 << shift;
        byte currentByte = bytes[byteIndex];
        bytes[byteIndex] = (byte) (currentByte | bitMask);
    }

    /**
     * Gets i-th bit in a bitmask.
     *
     * @param bytes Bitmask.
     * @param i Bit index (0-based)
     * @return 1 if bit is set, 0 otherwise
     * @since 1.0
     */
    public static int getBit(byte[] bytes, int i) {

        int byteIndex = (int) (i / 8d);
        if (byteIndex >= bytes.length) {
            throw new BtException("bit index is too large: " + i);
        }

        int bitIndex = i % 8;
        int shift = (7 - bitIndex);
        int bitMask = 0b1 << shift ;
        return (bytes[byteIndex] & bitMask) >> shift;
    }
}
