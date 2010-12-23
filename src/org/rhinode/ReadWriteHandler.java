package org.rhinode;

import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;

public abstract class ReadWriteHandler implements ReadStream, WriteStream, SelectableChannelDispatcher.Handler
{
    private final static BytePool readPool = new BytePool();
    private final ByteBufferOutputStream writeBuffer = new ByteBufferOutputStream(256*1024);
    private SelectionKey key;
    private boolean closeRequested = false;
    private StatefulDecoder decoder;

    public ReadWriteHandler(SelectionKey key) {
        this.key = key;
    }

    // for JS
    public ReadWriteHandler() {
    }
    public void setSelectionKey(SelectionKey key) {
        this.key = key;
    }

    // handler part
    public void execute(SelectionKey key) {
        try {
            if (key.isReadable()) {
                ByteBuffer data = null;
                try {
                    data = ReadWriteHandler.readPool.readFrom((ReadableByteChannel)key.channel());
                } catch(IOException x) {}
                if (data == null) {
                    onEnd();
                } else {
                    onData(this.decoder != null ? this.decoder.decode(data) : data);
                }
            } else if (key.isWritable()) {
                if (this.writeBuffer.writeTo((WritableByteChannel)key.channel()) == 0) {
                    key.interestOps(SelectionKey.OP_READ);
                    onDrain();
                    if(closeRequested) {
                        end();
                    }
                }
            }
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }
    
    public void write(byte[] data) throws IOException {
        this.writeBuffer.write(data);
    }
    public void write(ByteBuffer data) throws IOException {
        this.writeBuffer.write(data);
    }
    public void write(String data, String enc) throws IOException {
        this.writeBuffer.write(data,enc);
    }
    public void write(String data) throws IOException {
        this.write(data,"US-ASCII");
    }

    public void flush() {
        if ((this.key.interestOps() & SelectionKey.OP_WRITE) == 0)
            if (this.writeBuffer.writeTo((WritableByteChannel)this.key.channel()) > 0)
                this.key.interestOps(SelectionKey.OP_WRITE);
    }

    public void end(String data) throws IOException {
        end(data,"US-ASCII");
    }

    public void end(String data, String enc) throws IOException {
        write(data, enc);
        flush();
        end();
    }

    public void close() throws IOException {
        this.key.channel().close();
    }

    public void end() throws IOException {
        if (this.writeBuffer.size() > 0)
            this.closeRequested = true; 
        else
            close();
    }

    public void setEncoding(String enc) {
        this.decoder = new StatefulDecoder(enc);
    }

    public void onEnd() {
        try {
            end();
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }
    public abstract void onData(Object data);
    public void onDrain() {}
}
