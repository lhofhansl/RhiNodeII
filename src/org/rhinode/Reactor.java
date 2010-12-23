package  org.rhinode;

import java.nio.channels.*;
import java.io.IOException;
import java.util.*;

/*
 * Manages a set of Dispatchers.
 * Dispatchers are responsible for managing their events, sleeping, and deciding whether the app needs to be stay alive.
 */
public class Reactor implements Runnable {
    private final Selector sel;
    private List<Dispatcher> dispatchers = new ArrayList<Dispatcher>();

    public Reactor() throws IOException {
        sel = Selector.open();
    }

    public Selector getSelector() {
        return sel;
    }

    public void wakeup() {
        sel.wakeup();
    }

    public void add(Dispatcher q) {
        dispatchers.add(q);
    }

    public boolean remove(Dispatcher q) {
        return dispatchers.remove(q);
    }

    private long getNextWakeUp() {
        long next = 0;
        for (Dispatcher q : dispatchers) {
            long n = q.nextWakeup();
            if (n<=0) continue;
            next = next == 0 ? n : Math.min(next,n);
        }
        return next;
    }

    private boolean done() {
        for (Dispatcher q : dispatchers) {
            if (q.guard()) return false;
        }
        return true;
    }

    public void run() {
        try {
            while(!done()) {
                sel.select(getNextWakeUp());
                for (Dispatcher q : dispatchers) {
                    q.handleEvents();
                }
            }
            sel.close();
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }
}