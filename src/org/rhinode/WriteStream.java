package org.rhinode;

import java.nio.*;
import java.io.IOException;

public interface WriteStream {
    void write(byte[] data) throws IOException;
    void write(ByteBuffer data) throws IOException;
    void write(String data, String enc) throws IOException;
    void write(String data) throws IOException;
    void end(String data, String enc) throws IOException;
    void end(String data) throws IOException;
    void end() throws IOException;
    void flush() throws IOException;
}
