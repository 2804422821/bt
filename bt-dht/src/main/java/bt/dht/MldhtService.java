package bt.dht;

import bt.BtException;
import bt.metainfo.Torrent;
import bt.net.InetPeer;
import bt.net.Peer;
import bt.service.IRuntimeLifecycleBinder;
import com.google.common.io.Files;
import lbms.plugins.mldht.DHTConfiguration;
import lbms.plugins.mldht.kad.DHT;
import lbms.plugins.mldht.kad.DHT.DHTtype;
import lbms.plugins.mldht.kad.DHT.LogLevel;
import lbms.plugins.mldht.kad.DHTLogger;
import lbms.plugins.mldht.kad.tasks.PeerLookupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class MldhtService implements DHTService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MldhtService.class);

    static {
        try {
            DHT.setLogger(createLogger());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static DHTLogger createLogger() {
        return new DHTLogger() {
            @Override
            public void log(String message, LogLevel level) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("<" + level.name().toUpperCase() + "> " + message);
                }
            }

            @Override
            public void log(Throwable error, LogLevel level) {
                LOGGER.error("Unexpected DHT error", error);
            }
        };
    }

    private DHTConfiguration config;
    private DHT dht;

    public MldhtService(IRuntimeLifecycleBinder lifecycleBinder, DHTConfig config) {
        this.dht = new DHT(config.shouldUseIPv6()? DHTtype.IPV6_DHT : DHTtype.IPV4_DHT);
        this.config = toMldhtConfig(config);

        lifecycleBinder.onStartup(this::start);
        lifecycleBinder.onShutdown(this::shutdown);
    }

    private DHTConfiguration toMldhtConfig(DHTConfig config) {
        return new DHTConfiguration() {
            @Override
            public boolean isPersistingID() {
                return false;
            }

            @Override
            public Path getStoragePath() {
                return Files.createTempDir().toPath();
            }

            @Override
            public int getListeningPort() {
                return config.getListeningPort();
            }

            @Override
            public boolean noRouterBootstrap() {
                return !config.shouldUseRouterBootstrap();
            }

            @Override
            public boolean allowMultiHoming() {
                return false;
            }
        };
    }

    private void start() {
        if (!dht.isRunning()) {
            try {
                dht.start(config);
            } catch (SocketException e) {
                throw new BtException("Failed to start DHT", e);
            }
        }
    }

    private void shutdown() {
        dht.stop();
    }

    @Override
    public Stream<Peer> getPeers(Torrent torrent) {
        PeerLookupTask lookup;
        BlockingQueue<Peer> peers;
        try {
            lookup = dht.createPeerLookup(torrent.getTorrentId().getBytes());
            peers = new LinkedBlockingQueue<>();
            lookup.setResultHandler((k, p) -> {
                Peer peer = new InetPeer(p.getInetAddress(), p.getPort());
                peers.add(peer);
            });
            dht.getTaskManager().addTask(lookup);
        } catch (Throwable e) {
            LOGGER.error(String.format("Unexpected error in peer lookup: %s. Diagnostics:\n%s",
                    e.getMessage(), getDiagnostics()), e);
            throw e;
        }

        int characteristics = Spliterator.NONNULL;
        return StreamSupport.stream(() -> Spliterators.spliteratorUnknownSize(new Iterator<Peer>() {
            @Override
            public boolean hasNext() {
                return !lookup.isFinished();
            }

            @Override
            public Peer next() {
                try {
                    return peers.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Unexpectedly interrupted while waiting for next element", e);
                }
            }
        }, characteristics), characteristics, false);
    }

    private String getDiagnostics() {
        StringWriter sw = new StringWriter();
        dht.printDiagnostics(new PrintWriter(sw));
        return sw.toString();
    }
}
