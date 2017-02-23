package bt.data;

import org.junit.Test;

import static bt.TestUtil.assertExceptionWithMessage;
import static bt.data.ChunkDescriptorTestUtil.mockStorageUnit;
import static org.junit.Assert.assertEquals;

public class ChunkDescriptor_ExceptionsTest {

    @Test
    public void testExceptions_NoFiles() {
        assertExceptionWithMessage(
                it -> new DefaultChunkDescriptor(new StorageUnit[0], 0, 0, new byte[20], 0, false),
                "Failed to create chunk descriptor: no files");
    }

    @Test
    public void testExceptions_TooManyBlocks() {
        assertExceptionWithMessage(
                it -> new DefaultChunkDescriptor(new StorageUnit[1], 0, Long.MAX_VALUE, new byte[20], 1, false),
                "Integer overflow while constructing chunk: too many blocks");
    }

    @Test
    public void testExceptions_WriteEmptyBlock_NoException() {
        ChunkDescriptor chunk = new DefaultChunkDescriptor(new StorageUnit[]{mockStorageUnit(10)}, 0, 10, new byte[20], 5, false);
        chunk.writeBlock(new byte[0], 0);
    }

    @Test
    public void testExceptions_Write_NegativeOffset() {
        ChunkDescriptor chunk = new DefaultChunkDescriptor(new StorageUnit[]{mockStorageUnit(1)}, 0, 1, new byte[20], 1, false);
        assertExceptionWithMessage(
                it -> {chunk.writeBlock(new byte[1], -1); return null;},
                "Illegal arguments: offset (-1), length (1)");
    }

    @Test
    public void testExceptions_Read_NegativeOffset() {
        ChunkDescriptor chunk = new DefaultChunkDescriptor(new StorageUnit[]{mockStorageUnit(1)}, 0, 1, new byte[20], 1, false);
        assertExceptionWithMessage(
                it -> chunk.readBlock(-1, 1),
                "Illegal arguments: offset (-1), length (1)");
    }

    @Test
    public void testExceptions_Read_NegativeLength() {
        ChunkDescriptor chunk = new DefaultChunkDescriptor(new StorageUnit[]{mockStorageUnit(1)}, 0, 1, new byte[20], 1, false);
        assertExceptionWithMessage(
                it -> chunk.readBlock(0, -1),
                "Illegal arguments: offset (0), length (-1)");
    }

    @Test
    public void testExceptions_Read_ZeroLength_NoException() {
        ChunkDescriptor chunk = new DefaultChunkDescriptor(new StorageUnit[]{mockStorageUnit(1)}, 0, 1, new byte[20], 1, false);
        byte[] block = chunk.readBlock(0, 0);
        assertEquals(0, block.length);
    }

    @Test
    public void testExceptions_Read_RequestedTooMuchData() {
        ChunkDescriptor chunk = new DefaultChunkDescriptor(new StorageUnit[]{mockStorageUnit(1)}, 0, 1, new byte[20], 1, false);
        assertExceptionWithMessage(
                it -> chunk.readBlock(0, Integer.MAX_VALUE),
                "Insufficient data (offset: 0, requested block length: 2147483647)");
    }
}
