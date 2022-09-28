package com.walter.zkt.basic;

import com.walter.zkt.AbstractTest;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.zookeeper.CreateMode;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author walter.tan
 * @date 2022-08-26 17:16
 */
@Ignore
public class CuratorCacheTest extends AbstractTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void curatorCache(CuratorCache.Options options) throws Exception {
        try(CuratorCache cache = CuratorCache.build(client, "/pulsar", options)){
            CuratorCacheListener listener = CuratorCacheListener.builder()
                    .forCreates(node -> print(String.format("Node created: [%s]", node)))
                    .forChanges((oldNode, node) -> print(String.format("Node changed. Old: [%s] New: [%s]", oldNode, node)))
                    .forDeletes(oldNode -> print(String.format("Node deleted. Old value: [%s]", oldNode)))
                    .forInitialized(() -> print("Cache initialized"))
                    .build();

            // register the listener
            cache.listenable().addListener(listener, executor);

            // the cache must be started
            cache.start();

            CuratorTest test = new CuratorTest();
            test.create();
            test.get();
            test.set();
            test.get();
            test.set();
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/pulsar/topic2/partitions");
            test.delete();
        }

        Thread.sleep(5 * 1000);
    }

    /**
     * 仅监听当前节点
     * @throws Exception
     */
    @Test
    public void curatorCache() throws Exception {
        this.curatorCache(null);
    }

    /**
     * 监听当前节点及所有递归的子节点
     * @throws Exception
     */
    @Test
    public void curatorCacheWithSingleNode() throws Exception {
        this.curatorCache(CuratorCache.Options.SINGLE_NODE_CACHE);
    }
}
