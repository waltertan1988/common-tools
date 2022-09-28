package com.walter.zkt.core.registry;

import com.walter.zkt.core.ZktException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author walter.tan
 * @date 2022/9/12
 *
 */
public abstract class AbstractIdRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AbstractIdRegistry.class);
    /**
     * curator client
     */
    protected final CuratorFramework client;
    /**
     * curator lock client
     */
    protected final CuratorFramework lockClient;
    /**
     * topic name
     */
    protected final String topic;
    /**
     * assigned global id
     */
    protected volatile int globalId = -1;
    /**
     * session timeout from ZK server
     */
    protected final int negotiatedSessionTimeoutMs;
    /**
     * reentrant-lock that getting the next available id
     */
    protected final InterProcessMutex nextAvailableIdLock;
    /**
     * Timeout for reentrant-lock that getting the next available id
     */
    protected long nextAvailableIdLockTimeoutInMs = 10 * 1000L;
    /**
     * Supplier that helps customize instance ip, which may be anything but just can
     * distinguish different instances is ok
     */
    protected final Supplier<String> customIpSupplier;

    private String thisIp;

    protected AbstractIdRegistry(final CuratorFramework client, final CuratorFramework lockClient,
                                 final String topic, final long nextAvailableIdLockTimeoutInMs,
                                 final Supplier<String> customIpSupplier) throws Exception {
        if(Objects.isNull(client)){
            throw new IllegalArgumentException("client is required");
        }
        if(Objects.isNull(lockClient)){
            throw new IllegalArgumentException("lockClient is required");
        }
        if(Objects.isNull(topic) || topic.trim().isEmpty()){
            throw new IllegalArgumentException("topic is required");
        }

        this.client = client;
        this.lockClient = lockClient;
        this.topic = topic;
        this.negotiatedSessionTimeoutMs = this.getNegotiatedSessionTimeoutMs();
        this.nextAvailableIdLock = new AbstractIdRegistry.NextAvailableIdLock(this.client, this.getNextAvailableIdLockPath());
        if(nextAvailableIdLockTimeoutInMs >= 0){
            this.nextAvailableIdLockTimeoutInMs = nextAvailableIdLockTimeoutInMs;
        }
        this.customIpSupplier = Objects.nonNull(customIpSupplier) ? customIpSupplier : () -> {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new ZktException(e);
            }
        };
    }

    /**
     * Get the assigned global id
     * @return id
     */
    public Optional<Integer> getGlobalId() {
        return this.globalId == -1 ? Optional.empty() : Optional.of(this.globalId);
    }

    /**
     * Get negotiated session timeout from ZK server
     *
     * @return negotiated session timeout, which is different from that of client config
     * @throws Exception exception
     */
    public int getNegotiatedSessionTimeoutMs() throws Exception {
        if(this.negotiatedSessionTimeoutMs > 0){
            return this.negotiatedSessionTimeoutMs;
        }

        this.client.blockUntilConnected(30, TimeUnit.SECONDS);

        final int timeout = this.client.getZookeeperClient().getZooKeeper().getSessionTimeout();

        if(timeout <= 0){
            throw new ZktException("Cannot get negotiated session timeout");
        }

        return timeout;
    }

    /**
     * Distribution lock that ensure to get the next available id safely.
     */
    private class NextAvailableIdLock extends InterProcessMutex {
        public NextAvailableIdLock(final CuratorFramework client, final String path) {
            super(client, path);
        }

        @Override
        protected byte[] getLockNodeBytes() {
            return AbstractIdRegistry.this.getThisIp().getBytes(StandardCharsets.UTF_8);
        }
    }

    protected String getTopicPath(){
        return ZKPaths.makePath(ZKPaths.PATH_SEPARATOR, this.topic);
    }

    protected String getNextAvailableIdLockPath(){
        return ZKPaths.makePath(this.getTopicPath(), "nextAvailableIdLock");
    }

    protected String getThisIp() {
        if(Objects.isNull(this.thisIp)){
            this.thisIp = customIpSupplier.get();
        }
        return this.thisIp;
    }

    /**
     * Register for current instance
     * @throws Exception exception
     */
    protected void register() throws Exception{
        String ip = this.getThisIp();

        this.createTopicNode();

        this.registerBeforeProcess(ip);

        logger.info("start to acquire nextAvailableIdLock: [{}]", ip);
        if(!this.nextAvailableIdLock.acquire(this.nextAvailableIdLockTimeoutInMs, TimeUnit.MILLISECONDS)){
            throw new ZktException(String.format("cannot acquire nextAvailableIdLock: [%s]", ip));
        }
        try{
            int nextId = this.nextAvailableId();
            logger.info("createIpIdNodes start: [{}][{}]", ip, nextId);
            this.registerIpIdNodes(ip, nextId);
            globalId = nextId;
        }finally {
            this.nextAvailableIdLock.release();
            logger.info("finish release nextAvailableIdLock: [{}]", ip);
        }
    }

    /**
     * Create topic node in zookeeper
     * @throws Exception exception
     */
    protected abstract void createTopicNode() throws Exception;

    /**
     * Process before executing register
     * @param thisIp current instance ip
     * @throws Exception exception
     */
    protected void registerBeforeProcess(String thisIp) throws Exception{
        logger.info("registerBeforeProcess is ok. {}", thisIp);
    }

    /**
     * Calculate next available id
     *
     * @return next available id
     * @throws Exception exception
     */
    public abstract int nextAvailableId() throws Exception;

    /**
     * Register ip and id nodes in zookeeper
     * @param thisIp current instance ip
     * @param id id assigned to thisIp
     * @throws Exception exception
     */
    protected abstract void registerIpIdNodes(String thisIp, int id) throws Exception;

    /**
     * Verify whether the registration is expected
     * @param expectedInstanceCount expected instance count
     * @throws Exception exception
     */
    public void checkAfterAllReady(int expectedInstanceCount) throws Exception{
        logger.info("checkAfterAllReady start. expectedInstanceCount:{}", expectedInstanceCount);
    }

    /**
     * Unregister for current instance
     * @throws Exception exception
     */
    protected abstract void unregister() throws Exception;

    /**
     * Shutdown this registry, do some housekeeping and unregister for the current instance
     * @throws Exception exception
     */
    public void shutdown() throws Exception{
        this.unregister();
    }
}
