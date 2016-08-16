package bt.tracker;

import bt.BtException;
import bt.metainfo.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class MultiTracker implements Tracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTracker.class);

    private ITrackerService trackerService;
    private List<List<Tracker>> trackerTiers;

    MultiTracker(ITrackerService trackerService, AnnounceKey announceKey) {
        if (!announceKey.isMultiKey()) {
            throw new IllegalArgumentException("Not a multi key: " + announceKey);
        }
        this.trackerService = trackerService;
        this.trackerTiers = initTrackers(announceKey);
    }

    private List<List<Tracker>> initTrackers(AnnounceKey announceKey) {

        List<List<URL>> trackerUrls = announceKey.getTrackerUrls();
        List<List<Tracker>> trackers = new ArrayList<>(trackerUrls.size() + 1);

        for (List<URL> tier : trackerUrls) {
            List<Tracker> tierTrackers = new ArrayList<>(tier.size() + 1);
            for (URL trackerUrl : tier) {
                tierTrackers.add(new LazyTracker(() -> trackerService.getTracker(trackerUrl)));
            }
            // per BEP-12 spec each tier must be shuffled
            Collections.shuffle(tierTrackers);
            trackers.add(tierTrackers);
        }

        return trackers;
    }

    @Override
    public TrackerRequestBuilder request(Torrent torrent) {

        return new TrackerRequestBuilder(torrent.getTorrentId()) {

            @Override
            public TrackerResponse start() {
                return tryForAllTrackers(tracker -> getDelegate(tracker, torrent).start());
            }

            @Override
            public TrackerResponse stop() {
                return tryForAllTrackers(tracker -> getDelegate(tracker, torrent).stop());
            }

            @Override
            public TrackerResponse complete() {
                return tryForAllTrackers(tracker -> getDelegate(tracker, torrent).complete());
            }

            @Override
            public TrackerResponse query() {
                return tryForAllTrackers(tracker -> getDelegate(tracker, torrent).query());
            }

            private TrackerRequestBuilder getDelegate(Tracker tracker, Torrent torrent) {
                TrackerRequestBuilder delegate = tracker.request(torrent);

                int downloaded = getDownloaded();
                if (downloaded > 0) {
                    delegate.downloaded(downloaded);
                }

                int uploaded = getUploaded();
                if (uploaded > 0) {
                    delegate.uploaded(uploaded);
                }

                int left = getLeft();
                if (left > 0) {
                    delegate.left(left);
                }

                return delegate;
            }

            private TrackerResponse tryForAllTrackers(Function<Tracker, TrackerResponse> func) {

                List<TrackerResponse> responses = new ArrayList<>();

                for (List<Tracker> trackerTier : trackerTiers) {

                    TrackerResponse response;
                    Tracker currentTracker;

                    for (int i = 0; i < trackerTier.size(); i++) {
                        currentTracker = trackerTier.get(i);
                        response = func.apply(currentTracker);
                        responses.add(response);

                        if (response.isSuccess()) {
                            trackerTier.add(0, currentTracker);
                            return response;
                        } else if (response.getError().isPresent()) {

                            Throwable e = response.getError().get();
                            if (e instanceof IOException) {
                                LOGGER.warn("I/O error during interaction with the tracker", e);
                                // consider current tracker to be unreachable; continue with remaining trackers
                            } else {
                                throw new BtException("Unexpected error during interaction with the tracker", e);
                            }
                        } else {
                            throw new BtException("Unexpected error during interaction with the tracker; " +
                                    "message: " + response.getErrorMessage());
                        }
                    }
                }

                throw new BtException("All trackers failed; responses (in chrono order): " + responses);
            }
        };
    }

    private static class LazyTracker implements Tracker {

        private volatile Tracker delegate;
        private Supplier<Tracker> delegateSupplier;
        private final Object lock;

        LazyTracker(Supplier<Tracker> delegateSupplier) {
            this.delegateSupplier = delegateSupplier;
            lock = new Object();
        }

        @Override
        public TrackerRequestBuilder request(Torrent torrent) {
            return getDelegate().request(torrent);
        }

        private Tracker getDelegate() {

            if (delegate == null) {
                synchronized (lock) {
                    if (delegate == null) {
                        delegate = delegateSupplier.get();
                    }
                }
            }
            return delegate;
        }
    }
}
