package bt.net;

import bt.BtException;
import bt.metainfo.Torrent;
import bt.torrent.Bitfield;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Sends local bitfield to a newly connected remote peer.
 *
 * @since 1.0
 */
public class BitfieldConnectionHandler implements ConnectionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BitfieldConnectionHandler.class);

    private TorrentRegistry torrentRegistry;

    @Inject
    public BitfieldConnectionHandler(TorrentRegistry torrentRegistry) {
        this.torrentRegistry = torrentRegistry;
    }

    @Override
    public boolean handleConnection(PeerConnection connection) {
        Torrent torrent = torrentRegistry.getTorrent(connection.getTorrentId())
                // this should not happen, because presence of requested torrent
                // should have already been tested by other connection handlers
                .orElseThrow(() -> new BtException("Unknown torrent ID"));

        Optional<TorrentDescriptor> descriptorOptional = torrentRegistry.getDescriptor(torrent);
        if (descriptorOptional.isPresent() && descriptorOptional.get().isActive()) {
            Bitfield bitfield = descriptorOptional.get().getDataDescriptor().getBitfield();

            if (bitfield.getPiecesComplete() > 0) {
                bt.protocol.Bitfield bitfieldMessage = new bt.protocol.Bitfield(bitfield.getBitmask());
                connection.postMessage(bitfieldMessage);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Sending " + bitfieldMessage + " for " + connection.getRemotePeer());
                }
            }
            return true;
        }
        return false;
    }
}
