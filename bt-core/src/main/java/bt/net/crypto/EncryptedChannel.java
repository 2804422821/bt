package bt.net.crypto;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

class EncryptedChannel implements ByteChannel {

    private final ByteChannel delegate;
    private final Cipher cipherIn;
    private final Cipher cipherOut;

    EncryptedChannel(ByteChannel delegate, Cipher cipherIn, Cipher cipherOut) {
        this.delegate = delegate;
        this.cipherIn = cipherIn;
        this.cipherOut = cipherOut;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int read = 0;
        if (dst.hasRemaining()) {
            int position = dst.position();
            int limit = dst.limit();
            read = delegate.read(dst);
            if (read > 0) {
                dst.limit(dst.position());
                dst.position(position);
                byte[] bytes = new byte[dst.remaining()];
                dst.get(bytes);
                dst.limit(limit);
                dst.position(position);
                try {
                    bytes = cipherIn.update(bytes);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                dst.put(bytes);
            }
        }
        return read;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int written = 0;
        if (src.hasRemaining()) {
            int position = src.position();
            byte[] bytes = new byte[src.remaining()];
            src.get(bytes);
            src.position(position);
            try {
                bytes = cipherOut.update(bytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            src.put(bytes);
            src.position(position);
            written = delegate.write(src);
        }
        return written;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
