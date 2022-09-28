package com.walter.zkt.core.registry;

import com.walter.zkt.core.ZktException;
import com.walter.zkt.enums.DefaultTopicEnum;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * Registry that allocate and manage sequential (begin from 0) unique global id.
 *
 * This registry takes the first account into trying the best to ensure the allocated global id will not
 * be shared by different instances at the same time, so it will allocate an increased new id every time
 * the constructor method is call.
 *
 * Once the new id comes to the maximum threshold ({@link #maxGlobalId}), you can make your choice how
 * to handle it by setting {@link #globalIdExhaustedHandler}. By default, it will throw ZktException.
 *
 * @author walter.tan
 * @date 2022-09-14 13:21
 */
public class SequentialUniqueIdRegistry extends AbstractIdRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SequentialUniqueIdRegistry.class);
    /**
     * The maximum threshold of global id.
     */
    private final int maxGlobalId;

    private final IntUnaryOperator globalIdExhaustedHandler;

    public static class Builder {
        private final CuratorFramework client;
        private final CuratorFramework lockClient;
        private String topic = DefaultTopicEnum.SEQUENTIAL_UNIQUE_ID_REGISTRY.getNodeName();
        private long nextAvailableIdLockTimeoutInMs;
        private int maxGlobalId = Integer.MAX_VALUE;
        private IntUnaryOperator globalIdExhaustedHandler = maxGlobalIdThreshold -> {
            throw new ZktException(String.format("globalId is exhausted: %s", maxGlobalIdThreshold));
        };
        private Supplier<String> customIpSupplier;

        public Builder(CuratorFramework client, CuratorFramework lockClient){
            this.client = client;
            this.lockClient = lockClient;
        }

        public SequentialUniqueIdRegistry.Builder topic(String topic){
            this.topic = topic;
            return this;
        }

        public SequentialUniqueIdRegistry.Builder nextAvailableIdLockTimeoutInMs(long nextAvailableIdLockTimeoutInMs){
            this.nextAvailableIdLockTimeoutInMs = nextAvailableIdLockTimeoutInMs;
            return this;
        }

        public SequentialUniqueIdRegistry.Builder maxGlobalId(int maxGlobalId){
            if(maxGlobalId <= 0){
                throw new IllegalArgumentException("maxGlobalId must be positive");
            }
            this.maxGlobalId = maxGlobalId;
            return this;
        }

        public SequentialUniqueIdRegistry.Builder globalIdExhaustedHandler(IntUnaryOperator globalIdExhaustedHandler){
            if(Objects.isNull(globalIdExhaustedHandler)){
                throw new IllegalArgumentException("globalIdExhaustedHandler must not be null");
            }
            this.globalIdExhaustedHandler = globalIdExhaustedHandler;
            return this;
        }

        public SequentialUniqueIdRegistry.Builder customIpSupplier(Supplier<String> customIpSupplier){
            this.customIpSupplier = customIpSupplier;
            return this;
        }

        public SequentialUniqueIdRegistry build() throws Exception {
            SequentialUniqueIdRegistry registry = new SequentialUniqueIdRegistry(this);
            try{
                registry.register();
            }catch (final Exception ex){
                throw new ZktException("Fail to register.", ex);
            }
            return registry;
        }
    }

    public static SequentialUniqueIdRegistry.Builder builder(CuratorFramework client, CuratorFramework lockClient){
        return new SequentialUniqueIdRegistry.Builder(client, lockClient);
    }

    protected SequentialUniqueIdRegistry(SequentialUniqueIdRegistry.Builder builder) throws Exception {
        super(builder.client, builder.lockClient, builder.topic,
                builder.nextAvailableIdLockTimeoutInMs, builder.customIpSupplier);
        this.maxGlobalId = builder.maxGlobalId;
        this.globalIdExhaustedHandler = builder.globalIdExhaustedHandler;
    }

    @Override
    protected void createTopicNode() throws Exception {
        final String topicPath = this.getTopicPath();
        try{
            client.create()
                    .idempotent()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(topicPath, null);
        }catch (KeeperException.NodeExistsException ex){
            logger.info("topic exists, skip createTopicNode: {}", topicPath);
        }
    }

    @Override
    public int nextAvailableId() throws Exception {
        final byte[] currMaxIdByteArray = client.getData().forPath(getTopicPath());
        if(Objects.isNull(currMaxIdByteArray)){
            return 0;
        }

        int currMaxId = Integer.parseInt(new String(currMaxIdByteArray, StandardCharsets.UTF_8));
        int nextId = currMaxId + 1;
        if(nextId <= this.maxGlobalId) {
            return nextId;
        }

        return this.globalIdExhaustedHandler.applyAsInt(this.maxGlobalId);
    }

    @Override
    protected void registerIpIdNodes(String thisIp, int id) throws Exception {
        client.setData().idempotent()
                .forPath(getTopicPath(), String.valueOf(id).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void unregister() throws Exception {
        client.delete().idempotent().guaranteed()
                .forPath(ZKPaths.makePath(getTopicPath(), String.valueOf(this.globalId)));
    }
}
