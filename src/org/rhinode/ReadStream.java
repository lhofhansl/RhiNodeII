package org.rhinode;

import java.nio.*;
import java.io.IOException;

public interface ReadStream {
    void onData(Object data);
    void onEnd();
    void setEncoding(String enc);
}
