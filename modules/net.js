var event = require('event');

importPackage(org.rhinode.js);

importPackage(java.nio.channels);
importPackage(java.net);

// use a Java host object for performance
defineClass(JsReadWriteHandler, true);

function Server(cb) {
    if (cb)
        this.on("connection",cb);
}

Server.prototype = new event.Listener();
Server.prototype.listen = function(port, host, queue) {
    queue = queue || 128;
    this.ssc = ServerSocketChannel.open();
    this.ssc.configureBlocking(false);
    if(host)
        this.ssc.socket().bind(new InetSocketAddress(host,port), queue);
    else
        this.ssc.socket().bind(new InetSocketAddress(port), queue);

    var self = this;

    register(this.ssc,SelectionKey.OP_ACCEPT,new SelectableChannelDispatcher.Handler({execute:function (key) {
        var sc = key.channel().accept();
        sc.configureBlocking(false);

        var k = sc.register(key.selector(),SelectionKey.OP_READ);
        var h = new JsReadWriteHandler(k);
        k.attach(h);
        self.fire("connection",h);
    }}));
}

Server.prototype.close = function() {
    this.ssc.close();
    wakeup();
}

exports.Server = Server;

exports.createServer = function(cb) {
    return new Server(cb);
}
exports.createConnection = function(port, host, cb) {
    var sc = SocketChannel.open();
    sc.configureBlocking(false);
    var h = new JsReadWriteHandler(null);
    if (cb) h.on("connect", cb);
    var res = sc.connect(new java.net.InetSocketAddress(host||"127.0.0.1", port));
    if (res) {
        // if the connection succeded immediately
        var k = register(key.selector(),SelectionKey.OP_READ,h);
        h.selectionKey = k;
        h.fire("connect");
    } else {
        // connection did not succeed immediately (common case)
        register(sc, SelectionKey.OP_CONNECT, new SelectableChannelDispatcher.Handler({execute:function (key) {
            try {
                h.selectionKey = key;
                if(key.channel().finishConnect()) {
                    if (h.hasBufferedData() > 0)
                        key.interestOps(SelectionKey.OP_WRITE);
                    else
                        key.interestOps(SelectionKey.OP_READ);
                    // now setup the read/write handler on the same key
                    key.attach(h);
                    h.fire("connect");
                }
            } catch(ex) {
                h.fire("error",ex);
                h.close();
            }
        }}));
    }
    return h;
}