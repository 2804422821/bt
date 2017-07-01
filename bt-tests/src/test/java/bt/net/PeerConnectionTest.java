package bt.net;

import bt.metainfo.TorrentId;
import bt.protocol.Bitfield;
import bt.protocol.EncodingContext;
import bt.protocol.Handshake;
import bt.protocol.InvalidMessageException;
import bt.protocol.Message;
import bt.protocol.Request;
import bt.protocol.handler.MessageHandler;
import bt.test.protocol.ProtocolTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PeerConnectionTest {

    private static final ProtocolTest TEST = ProtocolTest.forBittorrentProtocol().build();

    private static final int BUFFER_SIZE = 2 << 6;

    private ServerSocketChannel serverChannel;
    private Server server;
    private SocketChannel clientChannel;

    @Before
    public void setUp() throws IOException {

        serverChannel = SelectorProvider.provider().openServerSocketChannel();
        serverChannel.bind(new InetSocketAddress(0));
        server = new Server(serverChannel);
        new Thread(server).start();

        SocketAddress localAddress = serverChannel.getLocalAddress();
        clientChannel = SelectorProvider.provider().openSocketChannel();
        clientChannel.connect(localAddress);
    }

    @Test
    public void testConnection() throws InvalidMessageException, IOException {
        Peer peer = mock(Peer.class);
        MessageHandler<Message> messageHandler = TEST.getProtocol();
        Supplier<Message> reader = new DefaultMessageReader(peer, clientChannel, messageHandler, BUFFER_SIZE);
        Consumer<Message> writer = new DefaultMessageWriter(clientChannel, peer, messageHandler, BUFFER_SIZE);
        MessageReaderWriter readerWriter = new DelegatingMessageReaderWriter(reader, writer);
        PeerConnection connection = new DefaultPeerConnection(peer, clientChannel, readerWriter);

        Message message;

        server.writeMessage(new Handshake(new byte[8], TorrentId.fromBytes(new byte[20]), PeerId.fromBytes(new byte[20])));
        message = connection.readMessageNow();
        assertNotNull(message);
        assertEquals(Handshake.class, message.getClass());

        server.writeMessage(new Bitfield(new byte[2 << 3]));
        message = connection.readMessageNow();
        assertNotNull(message);
        assertEquals(Bitfield.class, message.getClass());
        assertEquals(2 << 3, ((Bitfield) message).getBitfield().length);

        server.writeMessage(new Request(1, 2, 3));
        message = connection.readMessageNow();
        assertNotNull(message);
        assertEquals(Request.class, message.getClass());
        assertEquals(1, ((Request) message).getPieceIndex());
        assertEquals(2, ((Request) message).getOffset());
        assertEquals(3, ((Request) message).getLength());
    }

    @After
    public void tearDown() {
        try {
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Server implements Runnable, Closeable {

        private ServerSocketChannel channel;
        private volatile SocketChannel clientSocket;
        private final Object lock;

        Server(ServerSocketChannel channel) {
            this.channel = channel;
            lock = new Object();
        }

        @Override
        public void run() {
            try {
                synchronized (lock) {
                    clientSocket = channel.accept();
                }
            } catch (IOException e) {
                throw new RuntimeException("Unexpected I/O error", e);
            }
        }

        public void writeMessage(Message message) throws InvalidMessageException, IOException {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            assertTrue("Protocol failed to serialize message", TEST.getProtocol().encode(new EncodingContext(null), message, buffer));
            buffer.flip();
            synchronized (lock) {
                clientSocket.write(buffer);
            }
        }

        @Override
        public void close() throws IOException {
            if (clientSocket != null) {
                clientSocket.close();
            }
            channel.close();
        }
    }
}
