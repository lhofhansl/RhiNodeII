package org.rhinode.js;

import org.mozilla.javascript.annotations.*;
import org.mozilla.javascript.*;

import java.util.*;

import java.io.IOException;

/*
 * writer for an HTTP stream... NOT FINISHED, YET!
 */
public class JsHttpWriter extends EventListener {
    private static final long serialVersionUID = 1L;

    private String method;
    private String url;
    private String httpVersion;
    private Map<String,String> headers;
    private JsReadWriteHandler stream;

    private boolean first = true;
    private boolean chunked = false;
    private Boolean keepAlive;

    private static Map<Integer,String> HEADERS = new HashMap<Integer,String>();
    static {
        HEADERS.put(100,"Continue");
        HEADERS.put(200,"OK");
        HEADERS.put(201,"Created");
        HEADERS.put(204,"No Content");
        HEADERS.put(404,"Not Found");
        HEADERS.put(405,"Method Not Allowed");
        HEADERS.put(500,"Internal Server Error");
        // much more
    }

    @Override
    public String getClassName() { return "JsHttpWriter"; }

    public JsHttpWriter() {
    }

    private JsHttpWriter(String method, String url, Map<String,String> headers, String httpVersion, JsReadWriteHandler stream, Boolean keepAlive) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.httpVersion = httpVersion;
        this.stream = stream;
        this.keepAlive = keepAlive;
    }

    public static Scriptable jsConstructor(Context cx, Object[] args,
                                           Function ctorObj,
                                           boolean inNewExpr)
    {
        return new JsHttpWriter((String)args[0], // method
                                (String)args[1], // url
                                (Map<String,String>)args[2], // headers
                                (String)args[3], // http version
                                (JsReadWriteHandler)args[4], // stream
                                args.length > 4 ? (Boolean)args[5] : null);
    }

    @JSFunction
    public static void buffer(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        JsReadWriteHandler stream = ((JsHttpWriter)thisObj).stream;
        if (((JsHttpWriter)thisObj).chunked) {
        } else {
            if (args.length == 1 && args[0] instanceof String) {
                args = new Object[] {args[0], "UTF-8"};
            }
            // forward to stream
            JsReadWriteHandler.buffer(cx,stream,args,funObj);
        }
    }

    @JSFunction
    public static void write(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        buffer(cx,thisObj,args,funObj);
        ((JsHttpWriter)thisObj).stream.flush();
    }   

    @JSFunction
    public boolean flush() {
        return stream.flush();
    }

    public void writeHead(int statusCode, Map<String, String> hdrs) {
        stream.buffer("HTTP/"+this.httpVersion+" "+statusCode+" "+HEADERS.get(statusCode)+"\r\n");
        for(Map.Entry<String,String> h : hdrs.entrySet()) {
            stream.buffer(h.getKey()+": "+h.getValue()+"\r\n");
        }
        stream.buffer("\r\n");
    }

    private Map<String, String> augmentHaders(Map<String, String> hdrs) {
        return hdrs;
    }

    /*
HttpWriter.prototype.writeHead = function(statusCode, hdrs) {
    this.headers = this._augmentHeaders(hdrs);
    var a = "HTTP/"+this.httpVersion+" "+statusCode+" "+Headers[statusCode]+jsCRLF;
    this._writeHeaders(a, true); // buffer the header
}

function HttpWriter(method,url,headers,httpVersion,sock,req) {
    this.method = method.toUpperCase();
    this.url = url;
    if (headers != null) headers = this._augmentHeaders(headers);
    this.httpVersion = httpVersion;
    this.socket = sock;
    this.req = req;
    this.first = true;
}
HttpWriter.prototype = new event.Listener();

// Write some data. The data can be optionally encoded.
// The caller can also request to buffer the data before it is
// flushed to the kernel buffers.
// Data type allowed: String (js or Java), byte array, or a ByteBuffer
HttpWriter.prototype.write = function(data, enc, buffer) {
    this.first = false;
    if (typeof enc === "boolean") {
        buffer = enc;
        enc = undefined;
    }
    if (this.chunked) {
        let l, d;
        // determine encoded data and length
        // unfortunately Javascript and Java String are different
        if (typeof data === "string" || data instanceof String) {
            // get bytes for a Javascript string
            d = new java.lang.String(data).getBytes(enc||"UTF-8");
            l = d.length;
        } else if (data instanceof java.lang.String) {
            // java string
            d = data.getBytes(enc||"UTF-8");
            l = d.length;
        } else {
            // byte buffer or array
            d = data;
            l = d.length || d.remaining();
        }
        // always buffer whole chunks
        // call must ensure that the chunks fits into the send buffer
        this.socket.buffer(l.toString(16));
        this.socket.buffer(CRLF);
        this.socket.buffer(d);
        this.socket.buffer(CRLF);
        return buffer ? false : this.socket.flush();        
    } else if (buffer) {
        this.socket.buffer(data, enc || "UTF-8");
        return false;
    } else {
        return this.socket.write(data, enc || "UTF-8");
    }
}

HttpWriter.prototype.end = function(data, enc) {
    // buffer if there were no write and the response is chunked
    if (data) this.write(data,enc,this.first && this.chunked === true);
    if (this.chunked) {
        // write the last chunk (and flush the buffer)
        this.socket.write(LASTCHUNK);
    }
    // close the close if this is a server response and keep alive was not requested
    if (this.req && !this.req.isKeepAlive()) {
        this.socket.end();
    }
}

// write/buffer the header
HttpWriter.prototype.writeHead = function(statusCode, hdrs) {
    this.headers = this._augmentHeaders(hdrs);
    var a = "HTTP/"+this.httpVersion+" "+statusCode+" "+Headers[statusCode]+jsCRLF;
    this._writeHeaders(a, true); // buffer the header
}

// treat headers for correct http/1.1 behavior
HttpWriter.prototype._augmentHeaders = function(hdrs) {
    var contentLength=false,close=false,connection=false,transferEncoding=false;
    //hdrs = Object.create(hdrs);
    for(let h in hdrs) {
        if(!connection && connectionRegex.test(h)) {
            connection = true;
            close = closeRegex.test(hdrs[h]);
        } else if(!contentLength && contentLengthRegex.test(h)) {
            contentLength = true;
        } else if(!transferEncoding && transferEncodingRegex.test(h)) {
            transferEncoding = true;
            this.chunked = chunkedRegex.test(hdrs[h]);
        }
    }
    if (!this.req || this.req.isKeepAlive()) {
        if (!connection)
            hdrs.Connection = "keep-alive";
        if (!contentLength && !transferEncoding && !close) {
            hdrs["Transfer-Encoding"] = "chunked";
            this.chunked = true;
        }               
    } else if (!connection)
        hdrs.Connection = "close";

    return hdrs;
}

// write out or buffer the header
HttpWriter.prototype._writeHeaders = function(a,buffer) {
    for(var h in this.headers) {
        a += h+": "+this.headers[h]+jsCRLF;
    }
    a+=jsCRLF;
    if (buffer)
        this.socket.buffer(a);
    else
        this.socket.write(a);
}

// for clients
HttpWriter.prototype._writeRequestHead = function() {
    var a = this.method+" "+this.url+" HTTP/"+this.httpVersion+jsCRLF;
    this._writeHeaders(a); // don't buffer here
}

    */
}