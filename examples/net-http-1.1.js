// no processing, socket throughput test
var net = require('net');
var server = net.createServer(function (stream) {
    stream.setEncoding('UTF-8');
    stream.on('connect', function () {});
    stream.on('data', function (data) {
        //if(data.match("\r\n\r\n$"))
        stream.write('HTTP/1.0 200 OK\r\nConnection: Keep-Alive\r\nContent-Type: text/html\r\nContent-Length: 12\r\n\r\nHello World\n\r\n');
    });
    stream.on('end', function () {stream.end();});
});
server.listen(8000, 'localhost');
