package org.rhinode;

public interface Dispatcher
{
    /**
     * Return when the event loop should next wake up for this Dispatcher (0 if no wakeup is required)
     */
    long nextWakeup();

    /**
     * As long as this returns true, the main event loop will not finish.
     */
    boolean guard();

    /**
     * Perform all outstanding work (i.e. handle all events if any).
     * It is important that this method returns quickly if no events need to be handled.
     */
    void handleEvents();
}
