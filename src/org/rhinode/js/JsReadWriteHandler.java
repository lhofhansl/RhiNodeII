package org.rhinode.js;

import org.rhinode.*;

import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;

import java.util.*;

import org.mozilla.javascript.annotations.*;
import org.mozilla.javascript.*;

/*
 * Reader, Writer, and stream handler for Javascript.
 * Note: This class is *not* meant to used as a prototype for Javascript objects (it has local members)
 */
public class JsReadWriteHandler extends EventListener implements ReadStream, SelectableChannelDispatcher.Handler
{
    private static final long serialVersionUID = 1L;

    private final static BytePool readPool = new BytePool();
    private final ByteBufferOutputStream writeBuffer = new ByteBufferOutputStream(256*1024);
    private SelectionKey key;
    private boolean closeRequested = false;
    private StatefulDecoder decoder;

    @Override
    public String getClassName() { return "JsReadWriteHandler"; }

    public JsReadWriteHandler() {
    }

    // JS construtor
    public JsReadWriteHandler(NativeJavaObject key) {
        if (key != null)
            this.key = (SelectionKey)key.unwrap();
    }

    @JSSetter
    public void setSelectionKey(NativeJavaObject key) {
        this.key = (SelectionKey)key.unwrap();
    }

    @JSGetter
    public SelectionKey getSelectionKey() {
        return this.key;
    }

    // handler part
    public void execute(SelectionKey key) {
        try {
            if (key.isReadable()) {
                ByteBuffer data = null;
                try {
                    data = JsReadWriteHandler.readPool.readFrom((ReadableByteChannel)key.channel());
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
    
    @JSFunction
    public static void buffer(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        JsReadWriteHandler thisHandle = (JsReadWriteHandler)thisObj;
        if (args.length == 1) {
            if(args[0] instanceof ByteBuffer) {
                thisHandle.buffer((ByteBuffer)args[0]);
            } else if (args[0] instanceof NativeJavaArray) {
                thisHandle.buffer((byte[])((NativeJavaArray)args[0]).unwrap());
            } else if (args[0] instanceof byte[]) {
                thisHandle.buffer((byte[])args[0]);
            } else {
                thisHandle.buffer((String)args[0]);
            }
        } else if (args.length == 2) {
            thisHandle.buffer((String)args[0],(String)args[1]);
        } else {
            throw new RuntimeException("Wrong number of arguments");
        }
    }

    @JSFunction
    public static boolean write(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        buffer(cx,thisObj,args,funObj);
        return ((JsReadWriteHandler)thisObj).flush();
    }

    @JSFunction
    public boolean flush() {
        if (this.key == null) throw new RuntimeException("Stream not connected");

        if ((this.key.interestOps() & SelectionKey.OP_WRITE) != 0) return false;

        if (this.writeBuffer.size() > 0) {
            if (this.writeBuffer.writeTo((WritableByteChannel)this.key.channel()) > 0) {
                this.key.interestOps(SelectionKey.OP_WRITE);
                return false;
            }
        }
        return true;
    }

    @JSFunction
    public void pause() {
        this.key.interestOps(this.key.interestOps() & ~SelectionKey.OP_READ);
    }

    @JSFunction
    public void resume() {
        this.key.interestOps(this.key.interestOps() | SelectionKey.OP_READ);
    }

    @JSFunction
    public boolean hasBufferedData() {
        return this.writeBuffer.size() > 0;
    }

    @JSFunction
    public static void end(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        if(args.length > 0) {
            write(cx,thisObj,args,funObj);
        }
        ((JsReadWriteHandler)thisObj).end();
    }

    @JSFunction
    public void close() throws IOException {
        this.key.channel().close();
        this.key.selector().wakeup();
    }

    @JSFunction
    public void setEncoding(String enc) {
        this.decoder = new StatefulDecoder(enc);
    }

    // for Java
    public void buffer(ByteBuffer b) {
        this.writeBuffer.write(b);
    }
    public void buffer(byte[] b) throws IOException {
        this.writeBuffer.write(b);
    }
    public void buffer(String s) {
        this.writeBuffer.write(s);
    }
    public void buffer(String s, String enc) {
        this.writeBuffer.write(s,enc);
    }
    public void end() throws IOException {
        flush();
        if (this.writeBuffer.size() > 0)
            this.closeRequested = true; 
        else
            close();
    }

    public void onEnd() {
        fire("end");
    }
    public void onData(Object data) {
        fire("data", data);
    }
    public void onDrain() {
        fire("drain");
    }
}
