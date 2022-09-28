package com.walter.zkt.registry;

import com.google.common.collect.Lists;
import com.walter.zkt.AbstractTest;
import com.walter.zkt.core.registry.AbstractIdRegistry;
import com.walter.zkt.core.registry.SequentialReusableIdRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author walter.tan
 * @date 2022-09-07 10:28
 */
public class SequentialReusableIdRegistryTest extends AbstractTest {

    @Test
    public void sequentialReusableIdRegistry() throws Exception {
        final int instanceCount = 4;

        final List<CuratorFramework> clientList = Lists.newArrayList();
        final List<SequentialReusableIdRegistry> registryList = new CopyOnWriteArrayList<>();
        final List<CompletableFuture<Void>> completableFutures = Lists.newArrayList();
        final ExecutorService executor = Executors.newCachedThreadPool();

        // 1. Simulate different IP instances concurrently get globalId
        //    and each instance create its own SequentialReusableIdRegistry
        for (int i = 0; i < instanceCount; i++) {
            CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                final CuratorFramework testClient = newCuratorFramework();
                clientList.add(testClient);
                try {
                    SequentialReusableIdRegistry registry = SequentialReusableIdRegistry
                            .builder(testClient, testClient)
                            .topic("dummy_instance_id_registry")
                            .expBackOffBound(5)
                            .heartbeatIntervalInMs(10 * 1000)
                            .nextAvailableIdLockTimeoutInMs(instanceCount * 1000)
                            .customIpSupplier(() -> UUID.randomUUID().toString())
                            .heartbeatHandler(new SequentialReusableIdRegistry.HeartbeatHandler() {
                                @Override
                                public void onConnectionFailed(Exception ex) {
                                    try{
                                        super.onConnectionFailed(ex);
                                    }catch (Exception e){
                                        error(e.getMessage());
                                    }
                                }

                                @Override
                                public void onGlobalIdOccupied(int globalId, String occupiedByIp) {
                                    try {
                                        super.onGlobalIdOccupied(globalId, occupiedByIp);
                                    }catch (Exception ex){
                                        error(ex.getMessage());
                                    }
                                }
                            }).build();

                    registryList.add(registry);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, executor);

            completableFutures.add(cf);
        }
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();

        // 2. Verify all registry info
        for (int i = 0; i < instanceCount; i++) {
            AtomicInteger idx = new AtomicInteger(i);
            SequentialReusableIdRegistry registry = registryList.stream()
                    .filter(r -> Objects.equals(r.getGlobalId().orElse(-1), idx.get()))
                    .findFirst().orElse(null);
            Assert.assertNotNull(registry);

            int negotiatedSessionTimeout = registry.getNegotiatedSessionTimeoutMs();
            int heartbeatIntervalInMs = registry.getHeartbeatIntervalInMs();
            print(String.format("globalId: %s, negotiatedSessionTimeout: %s, heartbeatIntervalInMs: %s",
                    registry.getGlobalId(), negotiatedSessionTimeout, heartbeatIntervalInMs));
            Assert.assertTrue(negotiatedSessionTimeout > 0);
            Assert.assertTrue(heartbeatIntervalInMs > 0);
        }

        // 3. Check whether all registry heartbeats work
        int maxSessionTimeoutMs = 0;
        for (AbstractIdRegistry registry : registryList) {
            int timeout = registry.getNegotiatedSessionTimeoutMs();
            if(timeout > maxSessionTimeoutMs){
                maxSessionTimeoutMs = timeout;
            }
        }
        TimeUnit.MILLISECONDS.sleep(maxSessionTimeoutMs * 2);
        registryList.get(0).checkAfterAllReady(instanceCount);

        // 4. Close all components
        for (final AbstractIdRegistry registry : registryList) {
            registry.shutdown();
        }
        clientList.forEach(CloseableUtils::closeQuietly);
    }
}
