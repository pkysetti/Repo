package org.blueprism.replicated;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.*;

//import org.infinispan.notifications.cachelistener.annotation.CacheEntryEvicted;
//import org.infinispan.notifications.cachelistener.event.CacheEntryEvictedEvent;

@Listener(clustered = true)
public  class CacheListener {
    public Integer latency = null;

    private String cacheName = "[N/A] ";

    void showEvent(String form, CacheEntryEvent<Object, Object> event) {
        if (!event.isPre()) {
            MyLogger.log("[%s] " + form, this.cacheName, event.isOriginLocal() ? "L" : "R", event.getKey(), event.getValue());
        }
    }

    void addLatency(CacheEntryEvent<Object, Object> event) {
        if (latency != null && latency > 0 && !event.isOriginLocal()) {
            MyLogger.log("Holding event creator for %d miliseconds...", latency);
            long t0 = System.currentTimeMillis();
            try {
                Thread.sleep(latency);
            }
            catch (Exception e) {
                MyLogger.exception(e);
            }
            MyLogger.log("  => " + (System.currentTimeMillis() - t0) + " ms");
        }
    }

    public CacheListener(String cacheName) {
        super();
        this.cacheName = cacheName;
        MyLogger.log("Initialize event listner for [%s] ...", this.cacheName);
    }

    @CacheEntryCreated
    public void entryCreated(CacheEntryCreatedEvent<Object, Object> event) {
        addLatency(event);
        showEvent("-- [%s] Add item[%s => %s]", event);
    }

    @CacheEntryRemoved
    public void entryRemoved(CacheEntryRemovedEvent<Object, Object> event) {
        showEvent("-- [%s] Removed item[%s => %s]", event);
    }

    @CacheEntryModified
    public void entryModified(CacheEntryModifiedEvent<Object, Object> event) {
        showEvent("-- [%s] Modified item[%s => %s]", event);
    }

    @CacheEntryInvalidated
    public void entryInvalidated(CacheEntryInvalidatedEvent<Object, Object> event) {
        showEvent("-- [%s] Invalidated item[%s => %s]", event);
    }
/*
    @CacheEntryEvicted
    public void entryEvicted(CacheEntryEvictedEvent<Object, Object> event) {
        showEvent("\r-- [%s] Evicted item[%s => %s]", event);
    }
*/
}
