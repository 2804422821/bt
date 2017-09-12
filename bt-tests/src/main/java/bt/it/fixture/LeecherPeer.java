package bt.it.fixture;

import bt.Bt;
import bt.BtClientBuilder;
import bt.data.file.FileSystemStorage;
import bt.magnet.MagnetUri;
import bt.metainfo.Torrent;
import bt.runtime.BtClient;
import bt.runtime.BtRuntime;
import bt.tracker.AnnounceKey;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

class LeecherPeer extends SwarmPeer {

    private BtClient handle;
    private Path localRoot;
    private TorrentFiles files;

    LeecherPeer(Path localRoot,
                TorrentFiles files,
                Supplier<Torrent> torrentSupplier,
                BtRuntime runtime,
                boolean useMagnet,
                boolean stopWhenDownloaded) {

        super(runtime);

        Torrent torrent = torrentSupplier.get();

        BtClientBuilder builder = Bt.client(runtime).storage(new FileSystemStorage(localRoot));

        if (useMagnet) {
            // TODO: this is a bandaid fix; the issue of connecting to peers when using magnets should be solved in a different way
            AnnounceKey announceKey = torrent.getAnnounceKey()
                    .orElseThrow(() -> new IllegalStateException("Can't create magnet leecher for torrent without announce key"));
            if (announceKey.isMultiKey()) {
                throw new IllegalStateException("Can't create magnet leecher for torrent with multi announce key: " + announceKey);
            }
            MagnetUri magnetUri = MagnetUri.torrentId(torrent.getTorrentId()).tracker(announceKey.getTrackerUrl()).buildUri();
            builder = builder.magnet(magnetUri);
        } else {
            builder = builder.torrent(torrentSupplier);
        }

        if (stopWhenDownloaded) {
            builder = builder.stopWhenDownloaded();
        }

        this.handle = builder.build();

        this.localRoot = Objects.requireNonNull(localRoot);
        this.files = Objects.requireNonNull(files);
    }

    @Override
    public BtClient getHandle() {
        return handle;
    }

    @Override
    public boolean isSeeding() {
        // intentionally do not cache the result because
        // leecher may become seeder eventually
        return files.verifyFiles(localRoot);
    }
}
