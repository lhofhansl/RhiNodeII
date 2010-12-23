package org.rhinode;

import java.nio.channels.*;
import java.util.Set;

public class SelectableChannelDispatcher implements Dispatcher {
    Selector sel;
    public SelectableChannelDispatcher(Selector s) {
        this.sel = s;
    }

    public long nextWakeup() {
        return 0;
    }

    public boolean guard() {
        return sel.keys().size() > 0;
    }

    public void handleEvents() {
        Set<SelectionKey> keys = sel.selectedKeys();
        for (SelectionKey key : keys) {
            if (key.isValid()) ((Handler)key.attachment()).execute(key);
        }
        keys.clear();
    }

    ///

    // the channel will automatically be removed when it is closed
    public SelectionKey register(SelectableChannel channel, int ops, Handler handler) throws ClosedChannelException {
        return channel.register(sel,ops,handler);
    }

    public static interface Handler {
        void execute(SelectionKey key);
    }
}