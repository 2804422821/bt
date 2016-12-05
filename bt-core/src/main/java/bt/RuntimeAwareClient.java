package bt;

import bt.runtime.BtClient;
import bt.runtime.BtRuntime;
import bt.torrent.TorrentSession;
import bt.torrent.TorrentSessionState;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Client decorator that is able to attach itself to a runtime.
 *
 * @since 1.0
 */
class RuntimeAwareClient implements BtClient {

    private BtRuntime runtime;
    private BtClient delegate;

    public RuntimeAwareClient(BtRuntime runtime, BtClient delegate) {
        this.runtime = runtime;
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<?> startAsync() {
        ensureRuntimeStarted();
        attachToRuntime();
        return delegate.startAsync();
    }

    @Override
    public CompletableFuture<?> startAsync(Consumer<TorrentSessionState> listener, long period) {
        ensureRuntimeStarted();
        attachToRuntime();
        return delegate.startAsync(listener, period);
    }

    private void ensureRuntimeStarted() {
        if (!runtime.isRunning()) {
            runtime.startup();
        }
    }

    private void attachToRuntime() {
        runtime.attachClient(this);
    }

    @Override
    public void stop() {
        try {
            delegate.stop();
        } finally {
            detachFromRuntime();
        }
    }

    private void detachFromRuntime() {
        runtime.detachClient(this);
    }

    @Override
    public TorrentSession getSession() {
        return delegate.getSession();
    }
}
