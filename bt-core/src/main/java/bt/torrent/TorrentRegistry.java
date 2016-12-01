package bt.torrent;

import bt.data.Storage;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentId;
import bt.torrent.ITorrentDescriptor;

import java.util.Optional;

/**
 * Registry of all torrents known to the current runtime.
 *
 * @since 1.0
 */
public interface TorrentRegistry {

    /**
     * Get a torrent with a given torrent ID, if exists.
     *
     * @return {@link Optional#empty()} if this torrent ID is not known to the current runtime.
     * @since 1.0
     */
    Optional<Torrent> getTorrent(TorrentId torrentId);

    /**
     * Get a torrent descriptor for a given torrent, if exists.
     *
     * @return {@link Optional#empty()} if torrent descriptor hasn't been created yet.
     * @since 1.0
     */
    Optional<ITorrentDescriptor> getDescriptor(Torrent torrent);

    /**
     * Get an existing torrent descriptor for a given torrent
     * or create a new one if it does not exist.
     *
     * @param storage Storage to use for storing this torrent's files.
     *                Will be used when creating a new torrent descriptor.
     * @return Torrent descriptor
     * @since 1.0
     */
    ITorrentDescriptor getOrCreateDescriptor(Torrent torrent, Storage storage);
}
