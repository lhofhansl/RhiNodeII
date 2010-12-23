package org.rhinode.js;

import org.rhinode.ReadWriteHandler;
import java.nio.channels.SelectionKey;

/*
 * Convenience interface for Rhino.
 */
public class ReadWriteStream extends ReadWriteHandler {
    private DataCallback dataCallback;
    // runnable is fine, just need to be an interface with one no-arg method
    private Runnable endCallback;

    public ReadWriteStream(SelectionKey key) {
        super(key);
    }

    public void setOnDataCB(DataCallback c) {
        this.dataCallback = c;
    }

    public void setOnEndCB(Runnable r) {
        this.endCallback = r;
    }

    public void onData(Object data) {
        if (dataCallback != null)
            dataCallback.onData(data);
    }

    public void onEnd() {
        if (endCallback != null) 
            endCallback.run();
    }
}
