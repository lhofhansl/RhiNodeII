package org.rhinode;

import java.nio.channels.*;
import java.util.*;

public class TimerEventDispatcher implements Dispatcher {
    private long nTimers = 0; // needed because there's no way to check the number of tasks in a java.util.Timer
    private Reactor r;

    private Timer timer = new Timer("rhinode timers",true);
    private List<Runnable> callbacks = new ArrayList<Runnable>();

    private class MyTimerTask extends TimerTask {
        private Runnable run;
        private boolean remove;
        public MyTimerTask(Runnable run, boolean remove) {
            this.run = run;
            this.remove = remove;
        }
        public void run() {
            synchronized(TimerEventDispatcher.this) {
                callbacks.add(run);
                if (remove) {
                    nTimers--;
                }
                r.wakeup();
            }
        }
    }

    public TimerEventDispatcher(Reactor r) {
        this.r = r;
    }

    public TimerTask setTimeout(Runnable r, long delay) {
        nTimers++;
        MyTimerTask task = new MyTimerTask(r,true);
        timer.schedule(task, delay);
        return task;
    }

    public TimerTask setInterval(Runnable r, long period) {
        nTimers++;
        MyTimerTask task = new MyTimerTask(r,false);
        timer.schedule(task, period, period);
        return task;
    }

    public synchronized void nextTick(Runnable run) {
        callbacks.add(run);
        r.wakeup();
    }

    public boolean clearTimeout(TimerTask task) {
        boolean res = task.cancel();
        if(res) nTimers--;
        return res;
    }

    public boolean clearInterval(TimerTask task) {
        return clearTimeout(task);
    }

    public long nextWakeup() {
        return 0; // the mainlook is awakened on demand
    }
    public boolean guard() {
        return nTimers > 0;
    }
    public synchronized void handleEvents() {
        for(Runnable r : callbacks) {
            r.run();
        }
        callbacks.clear();
    }
}
