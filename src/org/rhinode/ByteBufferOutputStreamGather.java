package org.rhinode;

import java.io.*;
import java.nio.channels.*;
//import java.nio.charset.*;
import java.nio.*;

import java.util.*;

/*
 * OutputStream backed by a list ByteBuffers.
 * This is an experiment... In all cases I tested it is actually slower than
 * then copying the data to an existing (maybe growing) byte buffer.
 *
 * Keeping this around for now.
 */
public class ByteBufferOutputStreamGather extends OutputStream {
    private List<ByteBuffer> buffers = new LinkedList<ByteBuffer>();

    public ByteBufferOutputStreamGather() {
    }

    @Override
    public void write(byte[] b, int off, int len) {
        buffers.add(ByteBuffer.wrap(b,off,len));
    }

    public void write(byte[] b) {
        buffers.add(ByteBuffer.wrap(b));
    }
    
    public void write(ByteBuffer b) {
        buffers.add((ByteBuffer)b.flip());
    }
    
    public void write(int b) {
        throw new UnsupportedOperationException();
    }

    public void write(String data) {
        write(data,"US-ASCII");
    }

    public void write(String data, String enc) {
        byte[] a = null;
        try { a = data.getBytes(enc); } catch(UnsupportedEncodingException x) { throw new RuntimeException(x); }
        write(a);
    }

    public long size() {
        long s=0;
        for (ByteBuffer b : buffers) {
            s+=b.remaining();
        }
        return s;
    }

    // returns # of bytes that still need to be sent
    public long writeTo(GatheringByteChannel channel) {
        try {
            channel.write(buffers.toArray(new ByteBuffer[buffers.size()]));
        } catch (IOException x) {
            return -1;
        }
        for (Iterator<ByteBuffer> i = buffers.iterator(); i.hasNext(); ) {
            ByteBuffer b = i.next();
            if (b.remaining() == 0) {
                i.remove();
            } else
                break;
        }
        return size();
    }
}
