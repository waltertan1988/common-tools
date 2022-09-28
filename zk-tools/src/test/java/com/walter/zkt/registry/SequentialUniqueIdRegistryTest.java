package com.walter.zkt.registry;

import com.google.common.collect.Lists;
import com.walter.zkt.AbstractTest;
import com.walter.zkt.core.registry.AbstractIdRegistry;
import com.walter.zkt.core.registry.SequentialUniqueIdRegistry;
import com.walter.zkt.enums.DefaultTopicEnum;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author walter.tan
 * @date 2022-09-14 16:03
 */
public class SequentialUniqueIdRegistryTest extends AbstractTest {

    @Test
    public void sequentialUniqueIdRegistry() throws Exception {
        final int instanceCount = 4;

        final List<CuratorFramework> clientList = Lists.newArrayList();
        final List<SequentialUniqueIdRegistry> registryList = new CopyOnWriteArrayList<>();

        // 1. Simulate different IP instances concurrently get globalId
        for (int i = 0; i < instanceCount; i++) {
            final CuratorFramework testClient = newCuratorFramework();
            clientList.add(testClient);

            try{
                SequentialUniqueIdRegistry registry = SequentialUniqueIdRegistry
                        .builder(testClient, testClient)
                        .topic(DefaultTopicEnum.SEQUENTIAL_UNIQUE_ID_REGISTRY.getNodeName())
                        .nextAvailableIdLockTimeoutInMs(instanceCount * 1000)
                        .maxGlobalId(1000)
                        .globalIdExhaustedHandler(maxId -> 0)
                        .build();
                registryList.add(registry);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        // 2. Verify all registry info
        for (int i = 0; i < instanceCount; i++) {
            SequentialUniqueIdRegistry registry = registryList.get(i);
            Assert.assertNotNull(registry);

            int negotiatedSessionTimeout = registry.getNegotiatedSessionTimeoutMs();
            print(String.format("globalId: %s, negotiatedSessionTimeout: %s", registry.getGlobalId(), negotiatedSessionTimeout));
            Assert.assertTrue(negotiatedSessionTimeout > 0);
        }

        // 3. Check whether all are ready
        registryList.get(0).checkAfterAllReady(instanceCount);

        // 4. Close all components
        for (final AbstractIdRegistry registry : registryList) {
            registry.shutdown();
        }
        clientList.forEach(CloseableUtils::closeQuietly);
    }
}
