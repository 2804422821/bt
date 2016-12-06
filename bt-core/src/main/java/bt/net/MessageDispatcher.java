package bt.net;

import bt.metainfo.Torrent;
import bt.protocol.Message;
import bt.service.IRuntimeLifecycleBinder;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default single-threaded message dispatcher implementation.
 *
 *<p><b>Note that this class implements a service.
 * Hence, is not a part of the public API and is a subject to change.</b></p>
 */
public class MessageDispatcher implements IMessageDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDispatcher.class);

    private final Map<Peer, Collection<Consumer<Message>>> consumers;
    private final Map<Peer, Collection<Supplier<Message>>> suppliers;

    private TorrentRegistry torrentRegistry;

    @Inject
    public MessageDispatcher(IRuntimeLifecycleBinder lifecycleBinder,
                             IPeerConnectionPool pool,
                             TorrentRegistry torrentRegistry) {

        this.consumers = new ConcurrentHashMap<>();
        this.suppliers = new ConcurrentHashMap<>();
        this.torrentRegistry = torrentRegistry;

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "bt.net.message-dispatcher"));
        Worker worker = new Worker(pool);
        lifecycleBinder.onStartup(() -> executor.execute(worker));
        lifecycleBinder.onShutdown(this.getClass().getName(), worker::shutdown);
        lifecycleBinder.onShutdown(this.getClass().getName(), executor::shutdownNow);
    }

    private class Worker implements Runnable {

        private IPeerConnectionPool pool;

        private ReentrantLock lock;
        private Condition timer;

        private volatile boolean shutdown;

        Worker(IPeerConnectionPool pool) {

            this.pool = pool;

            lock = new ReentrantLock();
            timer = lock.newCondition();
        }

        @Override
        public void run() {
            while (!shutdown) {
                if (!consumers.isEmpty()) {
                    Iterator<Map.Entry<Peer, Collection<Consumer<Message>>>> iter = consumers.entrySet().iterator();
                    while (iter.hasNext()) {

                        Map.Entry<Peer, Collection<Consumer<Message>>> entry = iter.next();
                        Peer peer = entry.getKey();
                        Collection<Consumer<Message>> consumers = entry.getValue();

                        PeerConnection connection = pool.getConnection(peer);
                        if (connection != null && !connection.isClosed()) {

                            Optional<Torrent> torrent = torrentRegistry.getTorrent(connection.getTorrentId());
                            if (torrent.isPresent()) {
                                Optional<TorrentDescriptor> descriptor = torrentRegistry.getDescriptor(torrent.get());
                                if (descriptor.isPresent() && !descriptor.get().isActive()) {
                                    continue;
                                }
                            }

                            Message message = null;
                            try {
                                message = connection.readMessageNow();
                            } catch (Exception e) {
                                LOGGER.error("Error when reading message from peer: " + peer, e);
                                iter.remove();
                                suppliers.remove(peer);
                            }

                            if (message != null) {
                                for (Consumer<Message> messageConsumer : consumers) {
                                    try {
                                        messageConsumer.accept(message);
                                    } catch (Exception e) {
                                        LOGGER.warn("Error in message consumer", e);
                                    }
                                }
                            }
                        } else {
                            iter.remove();
                            suppliers.remove(peer);
                        }
                    }
                }

                if (!suppliers.isEmpty()) {
                    Iterator<Map.Entry<Peer, Collection<Supplier<Message>>>> iter = suppliers.entrySet().iterator();
                    while (iter.hasNext()) {

                        Map.Entry<Peer, Collection<Supplier<Message>>> entry = iter.next();
                        Peer peer = entry.getKey();
                        Collection<Supplier<Message>> suppliers = entry.getValue();

                        PeerConnection connection = pool.getConnection(peer);
                        if (connection != null && !connection.isClosed()) {

                            Optional<Torrent> torrent = torrentRegistry.getTorrent(connection.getTorrentId());
                            if (torrent.isPresent()) {
                                Optional<TorrentDescriptor> descriptor = torrentRegistry.getDescriptor(torrent.get());
                                if (descriptor.isPresent() && !descriptor.get().isActive()) {
                                    continue;
                                }
                            }

                            for (Supplier<Message> messageSupplier : suppliers) {
                                Message message = null;
                                try {
                                    message = messageSupplier.get();
                                } catch (Exception e) {
                                    LOGGER.warn("Error in message supplier", e);
                                }

                                if (message != null) {
                                    try {
                                        connection.postMessage(message);
                                    } catch (Exception e) {
                                        LOGGER.error("Error when writing message", e);
                                        iter.remove();
                                        consumers.remove(peer);
                                    }
                                }
                            }
                        } else {
                            iter.remove();
                            consumers.remove(peer);
                        }
                    }
                }

                lock.lock();
                try {
                    timer.await(1, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    lock.unlock();
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    @Override
    public synchronized void addMessageConsumer(Peer sender, Consumer<Message> messageConsumer) {
        Collection<Consumer<Message>> peerConsumers = consumers.get(sender);
        if (peerConsumers == null) {
            peerConsumers = ConcurrentHashMap.newKeySet();
            consumers.put(sender, peerConsumers);
        }
        peerConsumers.add(messageConsumer);
    }

    @Override
    public synchronized void addMessageSupplier(Peer recipient, Supplier<Message> messageSupplier) {
        Collection<Supplier<Message>> peerSuppliers = suppliers.get(recipient);
        if (peerSuppliers == null) {
            peerSuppliers = ConcurrentHashMap.newKeySet();
            suppliers.put(recipient, peerSuppliers);
        }
        peerSuppliers.add(messageSupplier);
    }
}
