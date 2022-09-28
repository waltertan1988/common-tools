package com.walter.concurrent.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

/**
 * @author walter.tan
 */
public class DefaultConfigureContext implements ConfigureContext {

    private final Map<String, ThreadPoolConfigure> map;

    public DefaultConfigureContext(){
        map = new ConcurrentHashMap<>(4);
        // 添加线程池配置信息
        ThreadPoolConfigure defaultThreadPoolConfigure = defaultThreadPoolConfigure();
        map.put(defaultThreadPoolConfigure.getKey(), defaultThreadPoolConfigure);
    }

    @Override
    public ThreadPoolConfigure getThreadPoolConfig(String key) {
        return map.get(key);
    }

    @Override
    public boolean tryAddThreadPoolConfigure(ThreadPoolConfigure configure) {
        return map.putIfAbsent(configure.getKey(), configure) == null;
    }

    /**
     * 默认线程池的配置
     * @return
     */
    private ThreadPoolConfigure defaultThreadPoolConfigure(){
        ThreadFactory threadFactory = new LoggingThreadFactory("ThreadPoolManager-worker");
        return new ThreadPoolConfigure(DEFAULT_THREAD_POOL_KEY,
                ThreadQueueType.SynchronousQueue.getValue(), 10, 100,
                0, 0, null, 0, 10,
                threadFactory, null);
    }
}
