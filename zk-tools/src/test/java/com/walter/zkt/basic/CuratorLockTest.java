package com.walter.zkt.basic;

import com.google.common.collect.Lists;
import com.walter.zkt.AbstractTest;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author walter.tan
 * @date 2022-09-11 19:35
 */
public class CuratorLockTest extends AbstractTest {

    static class AutoClosableInterProcessMutex extends InterProcessMutex implements AutoCloseable {

        public AutoClosableInterProcessMutex(CuratorFramework client, String path) {
            super(client, path);
        }

        @Override
        public void close() {
            try {
                this.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void sharedReentrantLock() throws InterruptedException {
        final int threadNum = 4;
        AtomicInteger count = new AtomicInteger(0);

        List<Thread> threadList = Lists.newArrayList();
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(() -> {
                try(AutoClosableInterProcessMutex lock = new AutoClosableInterProcessMutex(client, "/test/group0")) {
                    lock.acquire();
                    int cnt = count.get();
                    TimeUnit.MILLISECONDS.sleep(200);
                    count.set(cnt + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threadList.add(thread);

            thread.start();
        }

        for (Thread thread : threadList) {
            thread.join();
        }

        Assert.assertEquals(threadNum, count.get());
    }
}
