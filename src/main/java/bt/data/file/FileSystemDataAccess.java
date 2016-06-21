package bt.data.file;

import bt.BtException;
import bt.data.DataAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

class FileSystemDataAccess implements DataAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDataAccess.class);

    private File parent, file;
    private RandomAccessFile raf;
    private long size;

    private volatile boolean closed;

    FileSystemDataAccess(File root, List<String> pathElements, long size) {

        if (pathElements.isEmpty()) {
            throw new BtException("Can't create file access -- no path elements");
        }

        File parent = root;
        int len = pathElements.size();
        for (int i = 0; i < len - 1; i++) {
            parent = new File(parent, pathElements.get(i));
        }
        this.parent = parent;
        this.file = new File(parent, pathElements.get(len - 1));

        this.size = size;

        closed = true;
    }

    // TODO: this is temporary fix for verification upon app start
    // should be re-done (probably need additional API to know if data access is "empty")
    private boolean init(boolean create) {

        if (closed) {
            if (!parent.exists()) {
                if (create && !parent.mkdirs()) {
                    throw new BtException("Failed to create file access -- can't create (some of the) directories");
                } else {
                    return false;
                }
            }

            if (!file.exists()) {
                if (create) {
                    try {
                        if (!file.createNewFile()) {
                            throw new BtException("Failed to create file access -- " +
                                    "can't create new file: " + file.getAbsolutePath());
                        }
                    } catch (IOException e) {
                        throw new BtException("Failed to create file access -- unexpected I/O error", e);
                    }
                } else {
                    return false;
                }
            }

            try {
                raf = new RandomAccessFile(file, "rwd");
            } catch (FileNotFoundException e) {
                throw new BtException("Unexpected I/O error", e);
            }

            closed = false;
        }
        return true;
    }

    @Override
    public byte[] readBlock(long offset, int length) {

        if (closed) {
            if (!init(false)) {
                return new byte[length];
            }
        }

        if (offset < 0 || length < 0) {
            throw new BtException("Illegal arguments: offset (" + offset + "), length (" + length + ")");
        } else if (offset > size - length) {
            throw new BtException("Received a request to read past the end of file (offset: " + offset +
                    ", requested block length: " + length + ", file size: " + size);
        }

        try {
            raf.seek(offset);
            byte[] block = new byte[length];
            raf.read(block);
            return block;

        } catch (IOException e) {
            throw new BtException("Failed to read bytes (offset: " + offset +
                    ", requested block length: " + length + ", file size: " + size + ")", e);
        }
    }

    @Override
    public void writeBlock(byte[] block, long offset) {

        if (closed) {
            init(true);
        }

        if (offset < 0) {
            throw new BtException("Negative offset: " + offset);
        } else if (offset > size - block.length) {
            throw new BtException("Received a request to write past the end of file (offset: " + offset +
                    ", block length: " + block.length + ", file size: " + size);
        }

        try {
            raf.seek(offset);
            raf.write(block);

        } catch (IOException e) {
            throw new BtException("Failed to write bytes (offset: " + offset +
                    ", block length: " + block.length + ", file size: " + size + ")", e);
        }
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public String toString() {
        return "(" + size + " B) " + file.getPath();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                raf.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close file: " + file.getPath(), e);
            } finally {
                closed = true;
            }
        }
    }
}
