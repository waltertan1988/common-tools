package com.walter.concurrent.core;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理类
 * @author walter.tan
 */
@Slf4j
public class CustomThreadPoolManager {

    private static final ConcurrentHashMap<String, CustomThreadPool> THREAD_POOLS = new ConcurrentHashMap<>();

    private CustomThreadPoolManager(){}

    public static CustomThreadPool getThreadPool(ThreadPoolConfigure conf) {
        String key = conf.getKey();
        if (!THREAD_POOLS.containsKey(key)) {
            synchronized (CustomThreadPoolManager.class) {
                if (!THREAD_POOLS.containsKey(key)) {
                    THREAD_POOLS.put(key, new CustomThreadPool(conf));
                }
            }
        }
        return THREAD_POOLS.get(key);
    }

    public static Map<String, CustomThreadPool> getThreadPool() {
        return THREAD_POOLS;
    }

    public static void shutdown(String type) {
        CustomThreadPool pool = THREAD_POOLS.get(type);
        if (null == pool) {
            return;
        }
        pool.shutdown();
        THREAD_POOLS.remove(type);
    }

    public static void shutdownAll(long timeout, TimeUnit timeUnit) {
        List<CustomThreadPool> customThreadPoolList = new ArrayList<>(THREAD_POOLS.values());
        if (!customThreadPoolList.isEmpty()) {
            for (CustomThreadPool customThreadPool : customThreadPoolList) {
                log.info("thread pool is closing: {}", customThreadPool.getKey());
                customThreadPool.shutdown();
                try {
                    if (!customThreadPool.awaitTermination(timeout, timeUnit)) {
                        customThreadPool.shutdownNow();
                    }
                    log.info("thread pool is closed: {}", customThreadPool.getKey());
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for customThreadPool to be shutdown. key:" + customThreadPool.getKey());
                }
            }
        }
        THREAD_POOLS.clear();
    }
}
