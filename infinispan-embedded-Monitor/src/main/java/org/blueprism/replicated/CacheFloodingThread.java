package org.blueprism.replicated;

import org.infinispan.Cache;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheFloodingThread extends Thread {
    public static final ConcurrentHashMap<CacheFloodingThread, String> threads = new ConcurrentHashMap<>();

    Cache<Object, Object> target;
    int delay;
    int count;
    int lifespan;

    public CacheFloodingThread(Cache<Object, Object> target, String info, int count, int lifespan, int delay) {
        super();

        this.target = target;
        this.count = count;
        this.lifespan = lifespan;
        this.delay = delay * 1000;

        threads.put(this, info);

        start();
    }

    public void run() {
        try {
            while (true) {
                for (int n = 0; n < count; n++) {
                    target.put(String.format("User%04d_loginTime", n), new Date().toString(), lifespan, TimeUnit.SECONDS);
                }
                if (delay <= 0) {
                    break;
                }
                Thread.sleep(delay);
            }
        }
        catch(Exception e) {
        }
        threads.remove(this);
    }

    public static void list() {
        for (Map.Entry<CacheFloodingThread, String> entry : threads.entrySet()) {
            MyLogger.log("Thread [%d] - %s", entry.getKey().getId(), entry.getValue());
        }
    }

    public static void stop(long id) {
        for (CacheFloodingThread thread : threads.keySet()) {
            if (id == thread.getId()) {
                thread.interrupt();
                break;
            }
        }
    }

    public static void stopAll() {
        for (CacheFloodingThread thread : threads.keySet()) {
            thread.interrupt();
        }
    }
}