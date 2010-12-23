var net = require('net');
var s = net.createConnection(8000);

s.setEncoding("UTF-8");

s.on("connect", function() {
    s.on("data", function(data) {
        print(data);
        s.end();
    });
    s.write("GET / HTTP/1.0/r/n/r/n");
});
