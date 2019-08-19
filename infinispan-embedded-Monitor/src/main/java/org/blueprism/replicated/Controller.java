package org.blueprism.replicated;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.infinispan.Cache;
import org.infinispan.health.Health;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.protocols.DISCARD;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.ProtocolStack;
import org.springframework.util.SerializationUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class Controller {
    static final ObjectMapper MAPPER = new ObjectMapper();

    static Cache<Object, Object> cache;

    static final String[] commands = {
        "restart",
        "start",
        "quit",
        "members                   -- list of members (nodes)",
        "list                      -- list of all defined caches",
        "attach      <cache-name>  -- attach specific cache to monitoring session",
        "detach      [cache-name]  -- remove cache from current monitoring session",
        "silent      <cache-name>  -- remove listener from specific cache by name",
        "listen      <cache-name>  -- add listener to specific cache by name",
        "info        [cache-name]  -- show cache info",
        "clear       [cache-name]  -- clear attached cache or specific cache by name",
        "keys        [cache-name]  -- list all keys of cache",
        "put         <key>         -- put {'key' => 'datetime'} to current attached cache",
        "get         <key>         -- get value of 'key' from current attached cache",
        "remove      <key>         -- remove 'key' from attached cache",
        "latency     <miliseconds> -- add a delay in miliseconds to cache listener ",
        "flood       [number-of-items:default=100] [lifespan-in-second:default=1] [delay-in-second:default=0]",
        "threads                   -- list of active flooding threads",
        "stop        <thread-id>   -- stop a flooding thread by id",
        "print   -- prints the cache",
        "health  -- health of cache cluster",
        "crash   -- crashes the node",
        "uncrash",
        "stopper -- stops the cache",
        "starter -- starts the cache ",
        "help"
    };

    static final HashMap<String, Method> methods = new HashMap<String, Method>();

    static String[] params = {};

    static boolean active = true;


    public static String startConfiguration ;
    /**
     * Initialization
     */
    public static void init(String configuration) {

        startConfiguration  = configuration;


        for (String id : commands) {
            String cmd = id.split(" ")[0];
            try {
                methods.put(cmd, Controller.class.getMethod(cmd));
            }
            catch (Exception e) {
                MyLogger.exception(e);
            }
        }

        start();
        MyLogger.log("Attaching to default cache...");
        cache = MyCacheManager.getCache();

    }

    /**
     * Invoke a method using reflex method
     */
    public static void invoke(String[] args) throws Exception {
        String cmd = args[0];
        if (cmd.length() == 0) {
            help();
        }
        else if (methods.containsKey(cmd)) {
            params = args;
            try {
                MyLogger.debug("Invoking command '%s'", StringUtils.join(args, " "));
                methods.get(cmd).invoke(Controller.class);
            }
            catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof NoCacheAttachedException || t instanceof NoRequiredParameterException) {
                    MyLogger.log(t.getMessage());
                }
                else {
                    throw e;
                }
            }
        }
        else {
            MyLogger.error("Unknown command '%s', type 'help' for list of supported commands.", cmd);
        }
    }

    /**
     * Current active state of Controller
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Get parameter[index] as integer value, returns defValue if parameter[index] does not exist or fail to convert
     */
    private static int getParamAsInt(int index, int defValue) {
        try {
            return Integer.valueOf(params[index]);
        }
        catch (Exception e) {
            return defValue;
        }
    }

    static String convertToJSON(Object obj) {
        try {
            //return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            return MAPPER.writeValueAsString(obj);
        }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            MyLogger.error("Cannot convert to JSON");
        }
        return obj.toString();
    }

    /**
     * Usage: help
     */
    public static void help() {
        System.out.println("\nList of commands:");
        for (String cmd : commands) {
            System.out.println("\t" + cmd);
        }
        System.out.println();
        System.out.println("  Where:");
        System.out.println("\t<field>     mandatory field");
        System.out.println("\t[field]     optional field");
        System.out.println("\t     --     end of parameters, start of command explanation");
        System.out.println();
    }

    /**
     * Stop invocation by throwing exception if number of parameter does not meet requirement
     */
    private static void stopIfNoRequiredParameter() throws Exception {
        if (params.length < 2) {
            String message = "Type 'help' to see command details";
            for (String id : commands) {
                if (id.startsWith(params[0])) {
                    message = "Usage:\n\t" + id;
                    break;
                }
            }
            throw new NoRequiredParameterException(message);
        }
    }



    /**
     * Health
     * @throws Exception
     */
    public static void health() throws Exception {
        Health health = MyCacheManager.cacheManager.getHealth();

        System.out.println("--- cluster Name  ");
        System.out.println( health.getClusterHealth().getClusterName());

        System.out.println("Node Names ");
        System.out.println( health.getClusterHealth().getNodeNames());

        System.out.println("--- clusterHealth  Num Nodes  ");
        System.out.println( health.getClusterHealth().getNumberOfNodes());

        System.out.println("--- cluster Health ");
        System.out.println( health.getClusterHealth().getHealthStatus());

        //https://stackoverflow.com/questions/42654377/how-could-i-write-a-health-check-for-jgroups-under-inifinispan

        //https://connect2id.com/products/server/docs/guides/interpret-infinispan-jgroups-logs

        System.out.println("--- Trasnport View ");
        JGroupsTransport trans = (JGroupsTransport)cache.getAdvancedCache().getRpcManager().getTransport();

        JGroupsTransport t = (JGroupsTransport) MyCacheManager.cacheManager.getGlobalComponentRegistry().getComponent(Transport.class);
        JChannel channel = t.getChannel();


       // View view = View.create(channel.getAddress(), 100, channel.getAddress());
        GMS gms =  ((GMS) channel.getProtocolStack().findProtocol(GMS.class));


        gms.getNumberOfViews();

        System.out.println("** No of Views ");
        System.out.println( gms.getNumberOfViews());


        //trans.getChannel().connect("mycluster",Addresstarget,3000);

        ///trans.getChannel().connect("mycluster",Addresstarget,3000);



    }




    /**
     * crashes the cluster
     * @throws Exception
     */
    public static void crash() throws Exception {
        JGroupsTransport t = (JGroupsTransport) MyCacheManager.cacheManager.getGlobalComponentRegistry().getComponent(Transport.class);
        JChannel channel = t.getChannel();

        try {
            DISCARD discard = new DISCARD();
            discard.setDiscardAll(true);
            channel.getProtocolStack().insertProtocol(discard, ProtocolStack.Position.ABOVE, TP.class);
        } catch (Exception e) {
            MyLogger.log("Problems inserting discard", e);
            System.out.println("Problems inserting discard" + e);
            throw new RuntimeException(e);
        }
       // View view = View.create(channel.getAddress(), 100, channel.getAddress());
       // ((GMS) channel.getProtocolStack().findProtocol(GMS.class)).installView(view);
    }

    public static void uncrash() throws Exception {
        JGroupsTransport t = (JGroupsTransport) MyCacheManager.cacheManager.getGlobalComponentRegistry().getComponent(Transport.class);
        JChannel channel = t.getChannel();
        try {
           /* DISCARD discard = new DISCARD();
            discard.setDiscardAll(false);
            channel.getProtocolStack().insertProtocol(discard, ProtocolStack.Position.ABOVE, TP.class);*/
			channel.getProtocolStack().removeProtocol(DISCARD.class);
        } catch (Exception e) {
            MyLogger.log("Problems inserting discard", e);
            System.out.println("Problems inserting discard" + e);
            throw new RuntimeException(e);
        }
       // View view = View.create(channel.getAddress(), 100, channel.getAddress());
        //((GMS) channel.getProtocolStack().findProtocol(GMS.class)).installView(view);
    }








    /**
     * List of joining members/nodes
     */
    public static void members() throws Exception {
        List<String> nodes = MyCacheManager.getMembers();
        MyLogger.log("List of members:\n\t" + StringUtils.join(nodes, ",\n\t"));
    }

    /**
     * List of all defined caches
     */
    public static void list() throws Exception {
        String currentName = cache != null ? cache.getName() : "";
        List<String> cacheNames = new ArrayList<>();
        for (String name : MyCacheManager.getCacheNames()) {
            if (!currentName.equals(name.substring(2))) {
                cacheNames.add(name);
            }
            else {
                cacheNames.add("* " + name.substring(2));
            }
        }
        MyLogger.log("List of caches:\n\n\t" + StringUtils.join(cacheNames, ",\n\t") + "\n\n" +
                     "  + : cache is attached to this monitoring session\n" +
                     "  * : cache can be manipulated by 'put', 'get', 'clear', 'flood' commands\n");
    }

    /**
     * Usage: attach <cache-name>
     */
    public static void attach() throws Exception {
        stopIfNoRequiredParameter();

        String cacheName = params[1];
        boolean silent = params.length >= 3 && "silent".equals(params[2]);
        MyLogger.log("Attaching to cache [%s]...", cacheName);
        cache = MyCacheManager.getCache(cacheName, silent);
    }

    /**
     * Usage: detach <cache-name>
     */
    public static void detach() throws Exception {
        stopIfNoRequiredParameter();

        String cacheName = params[1];
        MyLogger.log("Detaching from cache [%s]...", cacheName);
        Cache<?, ?> candidate = MyCacheManager.detach(cacheName);
        if (candidate != null) {
            removeMyListener(candidate);
            if (candidate == cache) {
                cache = null;
            }
        }
    }

    /**
     * Usage:stopper
     * @throws Exception
     */
    public static void stopper() throws Exception {
        try {
            //MyCacheManager.cacheManager.getCache("repl-sync").stop();
            MyCacheManager.cacheManager.stop();

        } catch (Exception e) {
            System.out.print(e.getStackTrace().toString());
            throw new Exception(e);
        }
    }

    /**
     * starter
     * @throws Exception
     */
    public static void starter() throws Exception {
        try {
            //MyCacheManager.cacheManager.getCache("repl-sync").start();
            MyCacheManager.cacheManager.start();
        } catch (Exception e) {
            System.out.print(e.getStackTrace().toString());
            throw new Exception(e);
        }
    }


    /**
     * Find CacheListener instance from the candidate cache
     */
    @SuppressWarnings("unchecked")
    private static CacheListener getMyListener(Cache<?, ?> candidate) {
        CacheListener listener = null;
        if (candidate != null) {
            for (Object l : candidate.getListeners()) {
                if (l instanceof CacheListener) {
                    listener = (CacheListener) l;
                    break;
                }
            }
        }
        return listener;
    }

    /**
     * 
     */
    private static void removeMyListener(Cache<?, ?> candidate) {
        Object listener = getMyListener(candidate);
        if (listener != null) {
            candidate.removeListener(listener);
        }
    }

    /**
     * Usage: silent <cache-name>
     */
    public static void silent() throws Exception {
        stopIfNoRequiredParameter();

        String cacheName = params[1];
        if (MyCacheManager.isCacheAttached(cacheName)) {
            Cache<?, ?> candidate = MyCacheManager.getAttachedCache(cacheName);
            MyLogger.log("Stop event listener for [%s] ...", candidate.getName());
            removeMyListener(candidate);
        }
    }

    /**
     * Usage: listen <cache-name>
     */
    public static void listen() throws Exception {
        stopIfNoRequiredParameter();

        String cacheName = params[1];
        Cache<Object, Object> candidate = null;
        if (MyCacheManager.isCacheAttached(cacheName)) {
            candidate = MyCacheManager.getAttachedCache(cacheName);
            Object listener = getMyListener(candidate);
            if (listener == null) {
                candidate.addListener(new CacheListener(candidate.getName()));
            }
        }
        else {
            candidate = MyCacheManager.getCache(cacheName, false);
            if (cache == null) {
                cache = candidate;
            }
        }
    }

    private static void stopIfNoCurrentCache() throws NoCacheAttachedException {
        if (cache == null) {
            throw new NoCacheAttachedException("Please attach to a cache first");
        }
    }

    /**
     * Usage: info [cache-name]
     */
    public static void info() throws Exception {
        Cache<?, ?> candidate = null;
        if (params.length == 1) {
            stopIfNoCurrentCache();
            candidate = cache;
        }
        else {
            candidate = MyCacheManager.getCache(params[1], false);
        }

        int size = candidate.size();
        MyLogger.log("Cache [%s] is %s", candidate.getName(), candidate.getStatus());
        if (size > 0) {
            Set<?> keys = candidate.keySet();
            Object first = keys.iterator().next();
            Object second = candidate.get(first);
            String valClassName = second.getClass().getName();
            if (second instanceof byte[]) {
                try {
                    second = SerializationUtils.deserialize((byte[]) second);
                    valClassName = "byte[] { " + second.getClass().getName() + " }";
                }
                catch (Exception e) {
                    valClassName = "byte[]";
                }
            }
            MyLogger.log(" - Map of <%s, %s>", first.getClass().getName(), valClassName);
        }
        MyLogger.log(" - Number of items: %d", size);
    }

    /**
     * Usage: quit
     */
    public static void quit() {
        CacheFloodingThread.stopAll();

        active = false;
    }

    public static void restart() throws Exception {

        stopIfNoRequiredParameter();
        String cacheName = params[1];
        MyLogger.log("Detaching from cache [%s]...", cacheName);


        try {
                Cache<?, ?> candidate = MyCacheManager.detach(cacheName);
                if (candidate != null) {
                    removeMyListener(candidate);
                    if (candidate == cache) {
                        cache = null;
                    }
                }
                MyCacheManager.destroy();
            start();
        } catch (Exception e)
        {
            MyLogger.log("Detaching failed [%s]...", cacheName);
        }

    }

    public static void start(){
        String log4jConfigFile = System.getProperty("user.dir")+ File.separator +"config"
                + File.separator + "log4j.properties";
        PropertyConfigurator.configure(log4jConfigFile);

        System.out.println(">>>log4j"+log4jConfigFile);
        try {
            MyCacheManager.init(startConfiguration);
        }
        catch (Exception e){
            MyLogger.log(e.getMessage());
        }
    }

    /**
     * Clear a cache contents
     */
    public static void clear() throws Exception {
        if (params.length > 1) {
            cache = MyCacheManager.getCache(params[1], false);
        }
        stopIfNoCurrentCache();

        MyLogger.log("Clear contents of cache [%s]...", cache.getName());
        cache.clear();
    }

    /**
     * List all keys of a cache
     */
    public static void keys() throws Exception {
        stopIfNoCurrentCache();

        Set<?> keys = cache.keySet();
        if (keys.size() == 0) {
            MyLogger.log("Cache [%s] is empty", cache.getName());
        }
        else {
            MyLogger.log("Available entries (type = %s) of cache [%s]", keys.iterator().next().getClass().getName(), cache.getName());
            MyLogger.log("  %s", StringUtils.join(keys, ", "));
        }
    }

    /**
     * Put entry {'key', 'value'} to current attached cache
     */
    public static void put() throws Exception {
        stopIfNoCurrentCache();
        stopIfNoRequiredParameter();

        int ttl = getParamAsInt(2, 50);
        cache.put(params[1], new Date().toString(), ttl, TimeUnit.SECONDS);
    }

    static Object getJsonValue(Object value) {
        Object result = value;
        if (value instanceof byte[]) {
            try {
                result = SerializationUtils.deserialize((byte[]) value);
            }
            catch (Exception e) {
            }
        }

        return convertToJSON(result);
    }

    static Object getCacheEntryValue(Object key) {
        Object value = null;
        Set<?> keys = cache.keySet();
        if (keys.size() == 0) {
            MyLogger.log("Cache [%s] is empty", cache.getName());
        }
        else {
            if (keys.iterator().next() instanceof Integer) {
                key = Integer.valueOf(key.toString());
            }
            value = getJsonValue(cache.get(key));
        }
        return value;
    }

    /**
     * Get cache entry value
     */
    public static void get() throws Exception {
        stopIfNoCurrentCache();
        stopIfNoRequiredParameter();

        String key = params[1];
        Object value = getCacheEntryValue(key);
        if (value != null) {
            MyLogger.log("Cache [%s].entry(%s) = %s", cache.getName(), key, value);
        }
    }

    /**
     * Show cache contents
     */
    public static void print() throws Exception {
        stopIfNoCurrentCache();

        MyLogger.log("@ Cache [%s] values: ", cache.getName());
        for (Map.Entry<Object, Object> entry : cache.entrySet()) {
            MyLogger.log("  - %s => %s", entry.getKey(), getJsonValue(entry.getValue()));
        }
    }

    /**
     * Usage: remove <list-of-entry-name-in-csv>
     */
    public static void remove() throws Exception {
        stopIfNoCurrentCache();
        stopIfNoRequiredParameter();

        final String[] items = params[1].split(",");
        for (String key : items) {
            cache.remove(key);
        }
    }

    /**
     * Usage: latency <miliseconds>
     */
    public static void latency() throws Exception {
        stopIfNoCurrentCache();
        stopIfNoRequiredParameter();

        int lat = getParamAsInt(1, -1);
        if (lat >= 0) {
            CacheListener listener = getMyListener(cache);
            if (listener != null) {
                listener.latency = lat;
            }
        }
    }

    /**
     * Usage: flood [number-of-items:default=100] [lifespan-in-second:default=1]
     */
    public static void flood() throws Exception {
        stopIfNoCurrentCache();

        int count = getParamAsInt(1, 100);
        int lifespan = getParamAsInt(2, 1);
        int delay = getParamAsInt(3, 0);
        String loopMessage = delay > 0 ? String.format(" every %d seconds", delay) : "";

        MyLogger.log("Putting %d entries to cache [%s] with %d seconds expiration%s.", count, cache.getName(), lifespan, loopMessage);
        new CacheFloodingThread(cache, StringUtils.join(params, " "), count, lifespan, delay);
    }

    /**
     * List all active flooding threads
     */
    public static void threads() throws Exception {
        CacheFloodingThread.list();
    }

    /**
     * Usage: stop <thread-id>
     */
    public static void stop() throws Exception {
        stopIfNoRequiredParameter();

        long id = (long) getParamAsInt(1, -1);
        CacheFloodingThread.stop(id);
    }
}