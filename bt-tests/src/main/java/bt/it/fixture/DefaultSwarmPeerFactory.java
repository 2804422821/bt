package bt.it.fixture;

import bt.metainfo.Torrent;
import bt.runtime.BtRuntime;
import bt.runtime.BtRuntimeBuilder;
import bt.runtime.Config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.PrimitiveIterator;
import java.util.function.Supplier;
import java.util.stream.IntStream;

class DefaultSwarmPeerFactory implements SwarmPeerFactory {

    private Path root;
    private TorrentFiles torrentFiles;
    private Supplier<Torrent> torrentSupplier;
    private PrimitiveIterator.OfInt ports;

    DefaultSwarmPeerFactory(Path root, TorrentFiles torrentFiles, Supplier<Torrent> torrentSupplier, int startingPort) {
        this.root = root;
        this.torrentFiles = torrentFiles;
        this.torrentSupplier = torrentSupplier;
        this.ports = IntStream.range(startingPort, 65536).iterator();
    }

    @Override
    public SwarmPeer createSeeder(BtRuntimeBuilder runtimeBuilder) {
        int port = ports.next();
        BtRuntime runtime = createRuntime(runtimeBuilder, port);
        return new SeederPeer(createLocalRoot(port), torrentFiles, torrentSupplier, runtime);
    }

    @Override
    public SwarmPeer createLeecher(BtRuntimeBuilder runtimeBuilder) {
        return createLeecher(runtimeBuilder, false);
    }

    @Override
    public SwarmPeer createMagnetLeecher(BtRuntimeBuilder runtimeBuilder) {
        return createLeecher(runtimeBuilder, true);
    }

    private SwarmPeer createLeecher(BtRuntimeBuilder runtimeBuilder, boolean useMagnet) {
        int port = ports.next();
        BtRuntime runtime = createRuntime(runtimeBuilder, port);
        return new LeecherPeer(createLocalRoot(port), torrentFiles, torrentSupplier, runtime, useMagnet);
    }

    private BtRuntime createRuntime(BtRuntimeBuilder runtimeBuilder, int port) {
        Config config = runtimeBuilder.getConfig();
        config.setAcceptorAddress(localhostAddress());
        config.setAcceptorPort(port);
        return runtimeBuilder.build();
    }

    protected static InetAddress localhostAddress() {
        try {
            return Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            // not going to happen
            throw new RuntimeException("Unexpected error", e);
        }
    }

    private Path createLocalRoot(int port) {
        return root.resolve(String.valueOf(port));
    }
}
