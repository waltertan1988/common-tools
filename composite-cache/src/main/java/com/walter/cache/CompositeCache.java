package com.walter.cache;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author: walter.tan
 * @datetime: 2023/9/1 10:00
 */
public class CompositeCache<T> {

    private final AtomicReference<T> valueRef = new AtomicReference<>();

    private volatile Long lastUpdateTimestamp = 0L;

    private final long refreshIntervalInMs;

    private final long expireAt;

    private final Supplier<T> supplier;

    private final Consumer<T> afterLocalRefreshed;

    private final ReentrantLock reentrantLock = new ReentrantLock();

    /**
     * 组合缓存
     * @param refreshIntervalInMs 本地缓存刷新间隔 (毫秒)
     * @param expireAt 本地缓存失效时间点（时间戳）, <=0表示常驻内存（慎用）
     * @param initVal 本地缓存的初始值
     * @param supplier 数据源
     * @param afterLocalRefreshed 本地缓存刷新后的其他处理（可选）
     */
    public CompositeCache(long refreshIntervalInMs, long expireAt, T initVal, Supplier<T> supplier, Consumer<T> afterLocalRefreshed) {
        this.refreshIntervalInMs = refreshIntervalInMs;
        this.expireAt = expireAt;
        this.valueRef.set(initVal);
        this.supplier = supplier;
        this.afterLocalRefreshed = afterLocalRefreshed;
    }

    public T getValue(){
        long currentTs = System.currentTimeMillis();

        if(currentTs - lastUpdateTimestamp > this.refreshIntervalInMs){
            if(reentrantLock.tryLock()){
                try{
                    // 刷新本地
                    if(currentTs < expireAt){
                        T newVal = supplier.get();
                        valueRef.set(newVal);
                        if(Objects.nonNull(afterLocalRefreshed)){
                            afterLocalRefreshed.accept(newVal);
                        }
                        return newVal;
                    }else{
                        valueRef.set(null);
                    }

                    lastUpdateTimestamp = currentTs;
                }finally {
                    reentrantLock.unlock();
                }
            }
        }

        return valueRef.get();
    }
}
