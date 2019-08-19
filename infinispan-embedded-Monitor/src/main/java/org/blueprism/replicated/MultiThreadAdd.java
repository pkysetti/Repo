package org.blueprism.replicated;

import org.infinispan.Cache;
import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

public class MultiThreadAdd implements Runnable
{

    public static Cache<String,String> localCache ;
    private static int locali=0;
    public MultiThreadAdd(Cache<String,String> cache,int i){
        localCache = cache;
        locali= i;
    }

    public void run()
    {
        try
        {

            String key = "Simulated_Login"+locali;
            String val = "Simulated_Login"+ DateTime.now();
            localCache.put(key,val, 1000, TimeUnit.MILLISECONDS);
            System.out.println( Thread.currentThread().getId()+"---added in Thread --key--"+ key +"--value--"+val);

        }
        catch (Exception e)
        {
            // Throwing an exception 
            System.out.println ("Exception is caught");
        }
    }
} 