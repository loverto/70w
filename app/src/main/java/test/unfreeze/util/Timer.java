package test.unfreeze.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeoutException;

public class Timer {

    private final Handler handler;

    private AbstractRunnable abstractRunnable;

    public Timer(Looper looper) {
        this.handler = new Handler(looper);
    }

    public synchronized void start(AbstractRunnable abstractRunnable, long delayMillis) {
        this.handler.removeCallbacksAndMessages(null);
        this.handler.postDelayed(abstractRunnable, delayMillis);
        this.abstractRunnable = abstractRunnable;
    }

    public synchronized void disable() {
        this.handler.removeCallbacksAndMessages(null);
        this.abstractRunnable = null;
    }

    public synchronized boolean isNotNull() {
        return this.abstractRunnable != null;
    }

    public synchronized String getRunnableName() {
        return isNotNull() ? this.abstractRunnable.getRunnableName() : "";
    }

    public synchronized void destroy() {
        this.abstractRunnable = null;
    }

    public static abstract class AbstractRunnable implements Runnable {

        private String runnableName;

        public abstract void begin() throws TimeoutException;

        public abstract void end();

        public AbstractRunnable(String name) {
            this.runnableName = name;
        }

        public String getRunnableName() {
            return this.runnableName;
        }

        public final void run() {
            try {
                begin();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            end();
        }
    }
}
