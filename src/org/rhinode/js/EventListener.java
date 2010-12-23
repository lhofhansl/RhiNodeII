package org.rhinode.js;

import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.*;

import java.util.*;

/*
 * Implementation of the EventListener interface for Host Objects.
 * This is a helper for Java subclasses. It may also be used as a prototype for a new JavaScript objects.
 *
 * This needs to be a host object (rather than just a Java object accessed via reflection, so that handlers
 * can use the  this  reference. Sigh.
 */
public class EventListener extends ScriptableObject {
    private static final long serialVersionUID = 1L;

    // Java and JS constructor
    public EventListener() {
    }

    @Override
    public String getClassName() { return "EventListener"; }

    @JSFunction
    // arg1 = event name, arg2 = js-function
    public static Scriptable addEventListener(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        Set<Function> handlers = getHandlerMap(thisObj).get(args[0]);
        if (handlers == null) {
            handlers = new HashSet<Function>();
            getHandlerMap(thisObj).put((String)args[0], handlers);
        }
        handlers.add((Function)args[1]);
        return thisObj;
    }

    @JSFunction
    // arg1 = event name, [arg2 = function]
    public static Scriptable removeEventListener(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length == 1) {
            // remove all handlers for this event if no specific function was passed.
            getHandlerMap(thisObj).remove(args[0]);
        } else {
            // remove the passed function for this event
            Set<Function> handlers = getHandlerMap(thisObj).get(args[0]);
            if (handlers != null) {
                boolean first = true;
                for (Function f : (Function[])args) {
                    if (first) {
                        first = false;
                    } else {
                        handlers.remove(f);
                    }
                }
            }
        }
        return thisObj;
    }

    @JSFunction
    // trigger an event
    // arg1 = event name, arg2...n = arguments to handler function
    public static Scriptable fire(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        String event = (String)args[0];
        args = Arrays.copyOfRange(args,1,args.length);
        Set<Function> handlers = getHandlerMap(thisObj).get(event);
        if (handlers != null) {
            for(Function f : handlers) {
                f.call(cx, null, thisObj, args);
            }
        } else if ("error".equals(event)) {
            throw new RuntimeException(args[0].toString());
        }
        return thisObj;
    }

    @JSFunction
    // node.js compat
    public static Scriptable emit(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        return fire(cx,thisObj,args,funObj);
    }
    
    @JSFunction
    // node.js compat
    public static Scriptable addListener(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        return addEventListener(cx,thisObj,args,funObj);
    }

    @JSFunction
    // node.js compat
    public static Scriptable on(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        return addEventListener(cx,thisObj,args,funObj);
    }

    /*
     * Find the right prototype method
     * (not needed anymore?)
     *
    private static EventListener getPrototype(Scriptable obj) {
        while(obj != null && !(obj instanceof EventListener)) {
            obj = obj.getPrototype();
        }
        return (EventListener)obj;
    }
     */

    private static Map<String,Set<Function>> getHandlerMap(Scriptable obj) {
        Object res = obj.get("events",obj);
        if (res == Scriptable.NOT_FOUND) {
            res = new HashMap<String,Set<Function>>();
            obj.put("events", obj, res);
        }   
        return (Map<String,Set<Function>>)res;
    }

    // for use in Java subclasses
    public Scriptable fire(String event, Object... args) {
        Set<Function> handlers = getHandlerMap(this).get(event);
        if (handlers != null) {
            for(Function f : handlers) {
                f.call(Context.getCurrentContext(), null, this, args);
            }
        } else if ("error".equals(event)) {
            throw new RuntimeException(args[0].toString());
        }
        return this;
    }

    public Scriptable emit(String event, Object... args) {
        return fire(event, args);
    }
}
