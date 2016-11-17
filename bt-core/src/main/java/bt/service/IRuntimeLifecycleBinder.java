package bt.service;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Application lifecycle management API.
 * Provides hooks for all major types of lifecycle events.
 *
 * All thread workers and executors must be registered via this service.
 *
 * @since 1.0
 */
public interface IRuntimeLifecycleBinder {

    /**
     * Lifecycle events
     *
     * @since 1.0
     */
    enum LifecycleEvent {

        /**
         * Runtime startup
         *
         * @since 1.0
         */
        STARTUP,

        /**
         * Runtime shutdown
         *
         * @since 1.0
         */
        SHUTDOWN
    }

    /**
     * Register a hook to run upon runtime startup
     *
     * @since 1.0
     */
    void onStartup(Runnable r);

    /**
     * Register a hook to run upon runtime startup
     *
     * @param description Human-readable description of the hook
     * @since 1.0
     */
    void onStartup(String description, Runnable r);

    /**
     * Register a hook to run upon runtime shutdown
     *
     * @since 1.0
     */
    void onShutdown(Runnable r);

    /**
     * Register a hook to run upon runtime shutdown
     *
     * @param description Human-readable description of the hook
     * @since 1.0
     */
    void onShutdown(String description, Runnable r);

    /**
     * Visitor interface for inspecting all registered hooks for a particular lifecycle event.
     *
     * @param event Lifecycle event
     * @param consumer First parameter is hook's optional description,
     *                 second parameter is the hook itself.
     * @since 1.0
     */
    void visitBindings(LifecycleEvent event, BiConsumer<Optional<String>, Runnable> consumer);
}
