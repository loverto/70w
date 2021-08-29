package test.unfreeze.util;

import android.os.HandlerThread;
import android.os.Looper;

public class ChannelLooper {

    private static volatile Looper looper;

    static Looper getInstance() {
        if (looper == null) {
            synchronized (ChannelLooper.class) {
                if (looper == null) {
                    HandlerThread handlerThread = new HandlerThread("Channel Dispatcher");
                    handlerThread.start();
                    looper = handlerThread.getLooper();
                }
            }
        }
        return looper;
    }
}
