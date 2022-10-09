package com.walter.threadpool;

import com.walter.threadpool.core.CustomThreadPool;
import com.walter.threadpool.core.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author walter.tan
 */
@Slf4j
public class CustomThreadPoolTest extends BaseTests {

    @Autowired
    private ThreadPoolFactory threadPoolFactory;

    @Test
    public void execute() {
        log.info("main thread start");

        CustomThreadPool customThreadPool = threadPoolFactory.getDefaultThreadPool();
        for (int i = 0; i < 10; i++) {
            customThreadPool.execute(() -> {
                log.info("thread is running...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("thread is finish.");
            });
        }

        Assert.assertTrue(customThreadPool.getActiveCount() > 0);
        log.info("main thread end");
    }

    @Test(expected = RejectedExecutionException.class)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void executeReject() {
        log.info("main thread start");

        CustomThreadPool customThreadPool = threadPoolFactory.getThreadPool(CustomThreadPool.DEFAULT_KEY);
        customThreadPool.setPoolSize(1, 9);
        for (int i = 0; i < 10; i++) {
            customThreadPool.execute(() -> {
                log.info("thread is running...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("thread is finish.");
            });
        }
        log.info("main thread end");
    }

    @Test
    public void submitCallable() {
        log.info("main thread start");

        final int threadCount = 10;
        AtomicInteger counter = new AtomicInteger(0);
        List<Future<Integer>> futureList = new ArrayList<>();

        CustomThreadPool customThreadPool = threadPoolFactory.getDefaultThreadPool();
        for (int i = 0; i < threadCount; i++) {
            Future<Integer> future = customThreadPool.submit(() -> {
                log.info("thread is running...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("thread is finish.");
                return counter.incrementAndGet();
            });
            futureList.add(future);
        }

        int actual = futureList.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }).max(Integer::compareTo).orElse(-1);

        Assert.assertEquals(threadCount, actual);

        log.info("main thread end");
    }

    @Test
    public void submitRunnableWithTimeout() throws InterruptedException {
        log.info("main thread start");

        final int threadCount = 10;

        CustomThreadPool threadPool = threadPoolFactory.getDefaultThreadPool();
        Queue<Integer> resultQueue = new LinkedBlockingQueue<>();
        AtomicInteger counter = new AtomicInteger(0);
        for(int i=0; i<threadCount; i++){
            threadPool.submit(() -> {
                int cnt = counter.getAndIncrement();
                log.info("thread is running...");
                try {
                    Thread.sleep(cnt % 2 == 0 ? 1000 : 121000);
                    resultQueue.add(cnt);
                } catch (InterruptedException e) {
                    log.warn("thread is interrupted: {}", cnt, e);
                }
                log.info("thread is finish.");
            });
        }

        Thread.sleep(122000);

        Assert.assertTrue(resultQueue.size() < threadCount);

        log.info("main thread end, resultQueue.size:{}", resultQueue.size());
    }

    @Test
    public void submitRunnableWithExceptionHandler() throws InterruptedException {
        AtomicBoolean isOk = new AtomicBoolean(true);
        CustomThreadPool threadPool = threadPoolFactory.getDefaultThreadPool();
        threadPool.submit(() -> {
            new BigDecimal("xxx");
        }, ex -> {
            isOk.set(false);
            log.info("runnable fail.", ex);
        });

        Thread.sleep(5000);

        Assert.assertFalse(isOk.get());
    }
}
