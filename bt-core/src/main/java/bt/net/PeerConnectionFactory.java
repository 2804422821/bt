package bt.net;

import bt.metainfo.TorrentId;
import bt.protocol.Message;
import bt.protocol.handler.MessageHandler;
import bt.runtime.Config;
import bt.torrent.TorrentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Optional;

class PeerConnectionFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnectionFactory.class);

    private SocketChannelFactory socketChannelFactory;
    private IConnectionHandlerFactory connectionHandlerFactory;
    private SharedSelector selector;
    private MSEHandshakeProcessor cryptoHandshakeProcessor;

    public PeerConnectionFactory(MessageHandler<Message> messageHandler,
                                 SocketChannelFactory socketChannelFactory,
                                 IConnectionHandlerFactory connectionHandlerFactory,
                                 TorrentRegistry torrentRegistry,
                                 SharedSelector selector,
                                 Config config) {
        this.socketChannelFactory = socketChannelFactory;
        this.connectionHandlerFactory = connectionHandlerFactory;
        this.selector = selector;
        this.cryptoHandshakeProcessor = new MSEHandshakeProcessor(torrentRegistry, messageHandler,
                config.getEncryptionPolicy(), getBufferSize(config.getMaxTransferBlockSize()), config.getMsePrivateKeySize());
    }

    private static int getBufferSize(long maxTransferBlockSize) {
        if (maxTransferBlockSize > ((Integer.MAX_VALUE - 13) / 2)) {
            throw new IllegalArgumentException("Transfer block size is too large: " + maxTransferBlockSize);
        }
        return (int) (maxTransferBlockSize) * 2;
    }

    public Optional<PeerConnection> createOutgoingConnection(Peer peer, TorrentId torrentId) {
        Objects.requireNonNull(peer);

        InetAddress inetAddress = peer.getInetAddress();
        int port = peer.getPort();

        SocketChannel channel = null;
        try {
            channel = socketChannelFactory.getChannel(inetAddress, port);
            return createConnection(peer, torrentId, channel, false);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to establish ougoing connection to peer: {}. Reason: {} ({})",
                        peer, e.getClass().getName(), e.getMessage());
            }
            closeQuietly(channel);
            return Optional.empty();
        }
    }

    public Optional<PeerConnection> createIncomingConnection(Peer peer, SocketChannel channel) {
        try {
            return createConnection(peer, null, channel, true);
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to establish incoming connection from peer: {}. Reason: {} ({})",
                        peer, e.getClass().getName(), e.getMessage());
            }
            closeQuietly(channel);
            return Optional.empty();
        }
    }

    private Optional<PeerConnection> createConnection(Peer peer,
                                                             TorrentId torrentId,
                                                             SocketChannel channel,
                                                             boolean incoming) throws IOException {
        // sanity check
        if (!incoming && torrentId == null) {
            throw new IllegalStateException("Requested outgoing connection without torrent ID. Peer: " + peer);
        }

        channel.configureBlocking(false);

        PeerConnectionMessageWorker readerWriter = incoming ?
                cryptoHandshakeProcessor.negotiateIncoming(peer, channel)
                : cryptoHandshakeProcessor.negotiateOutgoing(peer, channel, torrentId);

        PeerConnection connection = new DefaultPeerConnection(peer, channel, readerWriter);
        ConnectionHandler connectionHandler;
        if (incoming) {
            connectionHandler = connectionHandlerFactory.getIncomingHandler();
        } else {
            connectionHandler = connectionHandlerFactory.getOutgoingHandler(torrentId);
        }
        boolean inited = initConnection(connection, connectionHandler);
        if (inited) {
            // use atomic wakeup-and-register to prevent blocking of registration,
            // if selection is resumed before call to register is performed
            // (there is a race between message dispatcher and current thread)
            selector.wakeupAndRegister(channel, SelectionKey.OP_READ, connection);
            return Optional.of(connection);
        } else {
            connection.closeQuietly();
            return Optional.empty();
        }
    }

    private boolean initConnection(PeerConnection newConnection, ConnectionHandler connectionHandler) {
        boolean success = connectionHandler.handleConnection(newConnection);
        if (success) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Successfully initialized newly established connection to peer: {}, handshake handler: {}",
                        newConnection.getRemotePeer(), connectionHandler.getClass().getName());
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to initialize newly established connection to peer: {}, handshake handler: {}",
                        newConnection.getRemotePeer(), connectionHandler.getClass().getName());
            }
        }
        return success;
    }

    private void closeQuietly(SocketChannel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e1) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Failed to close outgoing channel: {}. Reason: {} ({})",
                                channel.getRemoteAddress(), e1.getClass().getName(), e1.getMessage());
                    }
                } catch (IOException e2) {
                    // ignore
                }
            }
        }
    }
}
