package bt.net;

import bt.service.IPeerRegistry;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class PeerConnectionFactory {

    private SocketChannelFactory socketChannelFactory;
    private IPeerRegistry peerRegistry;

    public PeerConnectionFactory(IPeerRegistry peerRegistry, SocketChannelFactory socketChannelFactory) {
        this.peerRegistry = peerRegistry;
        this.socketChannelFactory = socketChannelFactory;
    }

    public PeerConnection createConnection(SocketChannel channel) throws IOException {

        Peer peer = getPeerForAddress((InetSocketAddress) channel.getRemoteAddress());
        return new PeerConnection(peer, channel);
    }

    private Peer getPeerForAddress(InetSocketAddress address) {
        return peerRegistry.getOrCreatePeer(address.getAddress(), address.getPort());
    }

    public PeerConnection createConnection(Peer peer) throws IOException {

        if (peer == null) {
            throw new NullPointerException("Peer is null");
        }

        InetAddress inetAddress = peer.getInetAddress();
        int port = peer.getPort();

        SocketChannel channel;
        try {
            channel = socketChannelFactory.getChannel(inetAddress, port);
        } catch (IOException e) {
            throw new IOException("Failed to create peer connection @ " + inetAddress + ":" + port, e);
        }

        return new PeerConnection(peer, channel);
    }
}
