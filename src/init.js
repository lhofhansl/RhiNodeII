var setTimeout;
var clearTimeout;
var setInterval;
var clearInterval;
var loop;
var wakeup;
var register;
var registerFile;
var console = {log:print};
var nextTick;

//
// One time setup when RhiNode is started
//
(function() {
    importPackage(org.rhinode);

    // setup Reator and a SelectableChannel and Timer dispatcher
    var reactor = new Reactor();
    var channels = new SelectableChannelDispatcher(reactor.selector);
    var timers = new TimerEventDispatcher(reactor);
    reactor.add(channels);
    reactor.add(timers);

    // interface
    wakeup = function() {
        reactor.wakeup();
    }
    
    register = function(channel, ops, handler) {
        return channels.register(channel,ops,handler);
    }

    setTimeout = function(cb,delay) {
        var args = arguments;
        return timers.setTimeout(function() {
            cb(Array.prototype.slice.call(args,2));
        }, delay);
    }

    setInterval = function(cb,period) {
        var args = arguments;
        return timers.setInterval(function() {
            cb(Array.prototype.slice.call(args,2));
        }, period);
    }

    nextTick = function(cb) {
        timers.nextTick(function() {
            cb(Array.prototype.slice.call(args,1));
        });
    }

    clearTimeout = function(task) {
        return timers.clearTimeout(task);
    };

    clearInterval = function(task) {
        return timers.clearInterval(task);
    };


    //

    loop = function() {
        reactor.run();
    }
})();
