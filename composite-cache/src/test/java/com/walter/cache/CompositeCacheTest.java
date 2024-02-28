package com.walter.cache;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author walter.tan
 * @date 2022-09-26 20:36
 */
public class CompositeCacheTest {
    private static final Logger logger = LoggerFactory.getLogger(CompositeCacheTest.class);
    // 模拟远程缓存
    private final DummyRemoteCache dummyRemoteCache = new DummyRemoteCache();

    private CompositeCache<Integer> compositeCache;

    @Before
    public void before() throws ParseException {
        // 本地缓存的初始值
        int initVal = 0;
        // 组合缓存（包括本地和远程）的刷新时间：10min
        long cacheRefreshIntervalMs = 10 * 60 * 1000;
        // 本地缓存的最终失效时间。当此值>0，则在此时间之后的本地缓存将直接返回null，以防止一直占用本地内存；<=0表示常驻本地内存
        long localCacheExpireAt = new SimpleDateFormat("yyyyMMdd").parse("20240401").getTime();

        // 构造组合缓存对象
        compositeCache = new CompositeCache<>(cacheRefreshIntervalMs, localCacheExpireAt, initVal, this.loadDataFromDB, dummyRemoteCache::putDataIntoRemoteCache);
    }

    @Test
    public void getValue() {
        ExecutorService executor = Executors.newCachedThreadPool();

        List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();

                // 查询数据
                Integer data = this.loadDataUsingCompositeCache();

                System.out.printf("data=%s, cost=%s(ms)%n", data, System.currentTimeMillis() - startTime);
            }, executor);
            completableFutureList.add(cf);
        }

        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
    }

    private Integer loadDataUsingCompositeCache(){
        // 优先从远程缓存获取数据
        Integer data = dummyRemoteCache.loadDataFromRemoteCache();
        if(Objects.nonNull(data)){
            return data;
        }

        // 使用组合缓存
        data = this.compositeCache.getValue();
        if(Objects.nonNull(data)){
            return data;
        }

        // 当前时间大于CompositeCache.expireAt时进入此逻辑
        data = this.loadDataFromDB.get();
        dummyRemoteCache.putDataIntoRemoteCache(data);

        return data;
    }

    // 模拟从DB查询数据
    private final Supplier<Integer> loadDataFromDB = () -> {
        try {
            logger.debug("loadDataFromDB start...");
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return 10000;
    };

    /**
     * 模拟远程缓存
     */
    private static class DummyRemoteCache {

        private final AtomicReference<Integer> dummyRemoteCache = new AtomicReference<>();

        /**
         * 模拟把数据写入到远程集中式缓存（如Redis）
         * @param newVal
         */
        public void putDataIntoRemoteCache(Integer newVal){
            try {
                logger.debug("putDataIntoRemoteCache start...");
                TimeUnit.MILLISECONDS.sleep(10);
                dummyRemoteCache.set(newVal);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 模拟从远程集中式缓存查询数据
         */
        public Integer loadDataFromRemoteCache() {
            try {
                logger.debug("loadDataFromRemoteCache start...");
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return dummyRemoteCache.get();
        }
    }
}
