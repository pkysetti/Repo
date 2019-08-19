package org.blueprism.replicated;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MyCacheManager {

    static EmbeddedCacheManager cacheManager;
    static HashMap<String, Cache<Object, Object>> attachedCaches = new HashMap<>();

    private MyCacheManager() { }

    public static Cache<Object, Object> getCache() {
        Cache<Object, Object> cache = cacheManager.getCache();
        if (cache != null) {
            String name = cache.getName();
            cache.addListener(new CacheListener(name));
            attachedCaches.put(name, cache);
        }
        
        return cache;
    }

    public static Cache<Object, Object> getAttachedCache(String name) {
        return attachedCaches.get(name);
    }

    public static Cache<Object, Object> getCache(String name, boolean silent) {
        Cache<Object, Object> cache = attachedCaches.get(name);
        if (cache == null) {
            cache = cacheManager.getCache(name);
            if (!silent) {
                cache.addListener(new CacheListener(name));
            }
            attachedCaches.put(name, cache);
        }
        return cache;
    }


    public static void init(String configuration) throws IOException {
        if (configuration == null) {
            String location = System.getProperty("user.dir")+"\\config";
            configuration = location + "\\infinispan.xml";
        }

        System.out.println(configuration);
        MyLogger.log("Loading configuration from: " + configuration);


        FileInputStream fis = new FileInputStream(configuration);
       // cacheManager = new DefaultCacheManager(fis);
        cacheManager = new DefaultCacheManager(configuration,true);
        cacheManager.addListener(new ClusterListener());

    }

    public static boolean isCacheAttached(String name) {
        return attachedCaches.containsKey(name);
    }

    public static Cache<Object, Object> detach(String cacheName) {
        return attachedCaches.remove(cacheName);
    }

    public static List<String> getCacheNames() {
        List<String> cacheNames = new ArrayList<>(cacheManager.getCacheNames());
        Collections.sort(cacheNames);

        for (int i = 0; i < cacheNames.size(); i++) {
            String name = cacheNames.get(i);
            cacheNames.set(i, (attachedCaches.containsKey(name) ? "+ " : "  ") + name);
        }

        return cacheNames;
    }

    public static List<String> getMembers() {
        List<String> members = new ArrayList<>();
        Object self = cacheManager.getAddress();
        for (Object node : cacheManager.getMembers()) {
            String name = node.toString();
            if (node.equals(self)) {
                name += " [me]";
            }
            members.add(name);
        }

        Collections.sort(members);
        return members;
    }

    public static void destroy() {
        cacheManager.stop();
        cacheManager = null;
    }
}
