package bt.event;

import bt.metainfo.TorrentId;
import bt.net.Peer;

import java.util.Objects;

public class PeerDisconnectedEvent extends BaseEvent implements TorrentEvent {

    private final TorrentId torrentId;
    private final Peer peer;

    PeerDisconnectedEvent(long id, long timestamp, TorrentId torrentId, Peer peer) {
        super(id, timestamp);
        this.torrentId = Objects.requireNonNull(torrentId);
        this.peer = Objects.requireNonNull(peer);
    }

    @Override
    public TorrentId getTorrentId() {
        return torrentId;
    }

    public Peer getPeer() {
        return peer;
    }

    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "] id {" + getId() + "}, timestamp {" + getTimestamp() +
                "}, torrent {" + torrentId + "}, peer {" + peer + "}";
    }
}
