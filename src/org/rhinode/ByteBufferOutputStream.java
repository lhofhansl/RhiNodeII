package org.rhinode;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.*;

// Outputstream backed by a growing ByteBuffer
public class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer buffer;
    private int max;

    public ByteBufferOutputStream(int max) {
        buffer = null;
        this.max = max;
    }
    
    @Override
    public void write(byte[] b, int off, int len) {
        ensureLen(len);
        buffer.put(b, off, len);
    }
    
    public void write(ByteBuffer b) {
        ensureLen(b.remaining());
    	buffer.put(b);
    }
    
    public void write(int b) {
        ensureLen(1);
        buffer.put((byte)b);
    }

    public void write(String data) {
        write(data,"US-ASCII");
    }

    public void write(String data, String enc) {
        byte[] a = null;
        try { a = data.getBytes(enc); } catch(UnsupportedEncodingException x) { throw new RuntimeException(x); }
        ensureLen(a.length);
        buffer.put(a);
    }

    public void clear() {
        // this is "optimized" for many low volume connections
        buffer = null;
    }
    public void reset() {
        clear();
    }
    
    public int size() {
        return buffer == null ? 0 : buffer.position();
    }

    public void compact() {
        buffer.flip();
        buffer = ByteBuffer.allocate(buffer.remaining()).put(buffer);
    }

    // returns # of bytes that still need to be sent
    public int writeTo(WritableByteChannel channel) {
        buffer.flip();
        try {
        int n = channel.write(buffer);
        } catch (IOException x) {
            return -1;
        }
        if (buffer.remaining() == 0) {
            reset();
        } else {
            // make sure the internal buffer is reset correctly
            buffer.compact();
        }
        return size();
    }
    
    private void ensureLen(int len) {
        if (buffer == null) {
            buffer = ByteBuffer.allocate(len);
        } else if(len > buffer.remaining() && buffer.capacity() < max) {
            int size = buffer.capacity();
            // at least double the size
            int nSize = Math.min(Math.max(size << 1, size + len), max);
            buffer.flip();
            buffer = ByteBuffer.allocate(nSize).put(buffer);
        }
    }
}
