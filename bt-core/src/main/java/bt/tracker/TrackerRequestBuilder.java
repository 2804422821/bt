package bt.tracker;

import bt.BtException;
import bt.metainfo.TorrentId;

import java.util.Objects;

/**
 * Base class for tracker request builders.
 *
 * @since 1.0
 */
public abstract class TrackerRequestBuilder {

    private TorrentId torrentId;

    private int uploaded;
    private int downloaded;
    private int left;

    /**
     * Create a tracker request builder for a given torrent ID
     *
     * @since 1.0
     */
    protected TrackerRequestBuilder(TorrentId torrentId) {
        this.torrentId = Objects.requireNonNull(torrentId);
    }

    /**
     * Announce to tracker, that the client is starting a torrent session.
     *
     * @return Tracker response
     * @since 1.0
     */
    public abstract TrackerResponse start();

    /**
     * Announce to tracker, that the client is stopping a torrent session.
     *
     * @return Tracker response
     * @since 1.0
     */
    public abstract TrackerResponse stop();

    /**
     * Announce to tracker, that the client has completed downloading the torrent.
     *
     * @return Tracker response
     * @since 1.0
     */
    public abstract TrackerResponse complete();

    /**
     * Query tracker for active peers.
     *
     * @return Tracker response
     * @since 1.0
     */
    public abstract TrackerResponse query();

    /**
     * Optionally set the amount of data uploaded during the current session.
     *
     * @param uploaded Amount of data uploaded since the last {@link #start()} request.
     * @return Builder
     * @since 1.0
     */
    public TrackerRequestBuilder uploaded(int uploaded) {
        if (uploaded <= 0) {
            throw new BtException("Invalid uploaded value: " + uploaded);
        }
        this.uploaded = uploaded;
        return this;
    }

    /**
     * Optionally set the amount of data downloaded during the current session.
     *
     * @param downloaded Amount of data downloaded since the last {@link #start()} request.
     * @return Builder
     * @since 1.0
     */
    public TrackerRequestBuilder downloaded(int downloaded) {
        if (downloaded <= 0) {
            throw new BtException("Invalid downloaded value: " + downloaded);
        }
        this.downloaded = downloaded;
        return this;
    }

    /**
     * Optionally set the amount of data left for the client to download.
     *
     * @param left Amount of data that is left for the client to complete the torrent download.
     * @return Builder
     * @since 1.0
     */
    public TrackerRequestBuilder left(int left) {
        if (left <= 0) {
            throw new BtException("Invalid left value: " + left);
        }
        this.left = left;
        return this;
    }

    TorrentId getTorrentId() {
        return torrentId;
    }

    int getUploaded() {
        return uploaded;
    }

    int getDownloaded() {
        return downloaded;
    }

    int getLeft() {
        return left;
    }
}
