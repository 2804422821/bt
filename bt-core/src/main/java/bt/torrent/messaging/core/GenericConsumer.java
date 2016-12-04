package bt.torrent.messaging.core;

import bt.protocol.Cancel;
import bt.protocol.Choke;
import bt.protocol.Interested;
import bt.protocol.KeepAlive;
import bt.protocol.NotInterested;
import bt.protocol.Unchoke;
import bt.torrent.annotation.Consumes;
import bt.torrent.messaging.MessageContext;

/**
 * Consumer that is responsible for processing
 * some of the very basic messages.
 *
 * @since 1.0
 */
public class GenericConsumer {

    private static final GenericConsumer instance = new GenericConsumer();

    static GenericConsumer consumer() {
        return instance;
    }

    @Consumes
    public void consume(KeepAlive keepAlive) {
        // ignore
    }

    @Consumes
    public void consume(Choke choke, MessageContext context) {
        context.getConnectionState().setPeerChoking(true);
    }

    @Consumes
    public void consume(Unchoke unchoke, MessageContext context) {
        context.getConnectionState().setPeerChoking(false);
    }

    @Consumes
    public void consume(Interested interested, MessageContext context) {
        context.getConnectionState().setPeerInterested(true);
    }

    @Consumes
    public void consume(NotInterested notInterested, MessageContext context) {
        context.getConnectionState().setPeerInterested(false);
    }

    @Consumes
    public void consume(Cancel cancel, MessageContext context) {
        context.getConnectionState().onCancel(cancel);
    }
}
