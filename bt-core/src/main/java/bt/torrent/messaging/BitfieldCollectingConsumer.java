package bt.torrent.messaging;

import bt.net.Peer;
import bt.protocol.Bitfield;
import bt.protocol.Have;
import bt.torrent.annotation.Consumes;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Accumulates received bitfields and haves without validating anything.
 * Used when exact torrent parameters like number of pieces is not known yet, e.g. when fetching metadata from peers.
 *
 * @since 1.3
 */
public class BitfieldCollectingConsumer {

    private ConcurrentMap<Peer, byte[]> bitfields;
    private ConcurrentMap<Peer, Set<Integer>> haves;

    public BitfieldCollectingConsumer() {
        this.bitfields = new ConcurrentHashMap<>();
        this.haves = new ConcurrentHashMap<>();
    }

    @Consumes
    public void consume(Bitfield bitfieldMessage, MessageContext context) {
        bitfields.put(context.getPeer(), bitfieldMessage.getBitfield());
    }

    @Consumes
    public void consume(Have have, MessageContext context) {
        Peer peer = context.getPeer();
        Set<Integer> peerHaves = haves.get(peer);
        if (peerHaves == null) {
            peerHaves = ConcurrentHashMap.newKeySet();
            haves.put(peer, peerHaves);
        }
        peerHaves.add(have.getPieceIndex());
    }

    public Map<Peer, byte[]> getBitfields() {
        return bitfields;
    }

    public Map<Peer, Set<Integer>> getHaves() {
        return haves;
    }
}
