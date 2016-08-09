package bt.service;

import bt.metainfo.Torrent;
import bt.net.InetPeer;
import bt.net.Peer;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PeerRegistry implements IPeerRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerRegistry.class);

    private Set<PeerSourceFactory> peerSourceFactories;
    private Map<Torrent, List<Consumer<Peer>>> consumers;
    private final Peer localPeer;

    @Inject
    public PeerRegistry(IRuntimeLifecycleBinder lifecycleBinder, INetworkService networkService,
                        IdService idService, Set<PeerSourceFactory> peerSourceFactories) {

        this.peerSourceFactories = peerSourceFactories;
        consumers = new ConcurrentHashMap<>();
        localPeer = new InetPeer(networkService.getInetAddress(), networkService.getPort(), idService.getLocalPeerId());

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Peer Registry"));
        lifecycleBinder.onStartup(() -> executor.scheduleAtFixedRate(this::collectAndVisitPeers, 1, 5, TimeUnit.SECONDS));
        lifecycleBinder.onShutdown(this.getClass().getName(), executor::shutdownNow);
    }

    private void collectAndVisitPeers() {

        for (Torrent torrent : consumers.keySet()) {

            List<Consumer<Peer>> peerConsumers = consumers.get(torrent);
            for (PeerSourceFactory peerSourceFactory : peerSourceFactories) {

                PeerSource peerSource = peerSourceFactory.getPeerSource(torrent);
                try {
                    if (peerSource.isRefreshable() && peerSource.refresh()) {

                        Collection<Peer> discoveredPeers = peerSource.query();
                        for (Peer peer : discoveredPeers) {
                            Iterator<Consumer<Peer>> iter = peerConsumers.iterator();
                            while (iter.hasNext()) {
                                Consumer<Peer> consumer = iter.next();
                                try {
                                    consumer.accept(peer);
                                } catch (Exception e) {
                                    LOGGER.warn("Error in peer consumer", e);
                                    iter.remove();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error when querying peer source: " + peerSource, e);
                }
            }
        }
    }

    @Override
    public Peer getLocalPeer() {
        return localPeer;
    }

    @Override
    public Peer getOrCreatePeer(InetAddress inetAddress, int port) {
        return new InetPeer(inetAddress, port);
    }

    @Override
    public void addPeerConsumer(Torrent torrent, Consumer<Peer> consumer) {

        List<Consumer<Peer>> peerConsumers = consumers.get(torrent);
        if (peerConsumers == null) {
            peerConsumers = new ArrayList<>();
            List<Consumer<Peer>> existing = consumers.putIfAbsent(torrent, peerConsumers);
            if (existing != null) {
                peerConsumers = existing;
            }
        }
        peerConsumers.add(consumer);
    }
}
