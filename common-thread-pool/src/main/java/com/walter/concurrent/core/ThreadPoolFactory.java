package com.walter.concurrent.core;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author walter.tan
 */
@Slf4j
public class ThreadPoolFactory {

    private final ConfigureContext configureContext;

    public ThreadPoolFactory(ConfigureContext configureContext){
        this.configureContext = configureContext;
    }

    public CustomThreadPool getDefaultThreadPool() {
        //使用线程池工具,使用默自定义线程池类型
        ThreadPoolConfigure conf = configureContext.getDefaultThreadPoolConfig();
        return CustomThreadPoolManager.getThreadPool(conf);
    }

    public CustomThreadPool getThreadPool(String key) {
        //使用线程池工具,使用自定义线程池类型
        ThreadPoolConfigure conf = configureContext.getThreadPoolConfig(key);
        return CustomThreadPoolManager.getThreadPool(conf);
    }

    public void shutdownAll(long timeout, TimeUnit timeUnit) {
        CustomThreadPoolManager.shutdownAll(timeout, timeUnit);
        log.info("ThreadPoolFactory has shutdown all thread pools.");
    }
}
