package com.walter.zkt.core.registry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.walter.zkt.core.ZktException;
import com.walter.zkt.enums.DefaultTopicEnum;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.OpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Registry that allocate and manage sequential(begin from 0) reusable global id.
 *
 * This registry takes the first account into trying the best to ensure the allocated global id should have
 * at least one instance registered for it by reusing the minimum global id that not being occupied.
 *
 * <p>
 *  Normally one global id, one instance ip. But there is conflict case that if the registered instance loss session
 *  by network issues and there is a new instance comes and registry at the same time, then the new and the old
 *  instances will share the same global id and the old instance will throw ZktException in heartbeat checking by
 *  default. You may extend {@link SequentialReusableIdRegistry.HeartbeatHandler} to decide how the old instance to
 *  handle this conflict case.
 * </p>
 *
 * @author walter.tan
 * @date 2022/8/27
 */
public class SequentialReusableIdRegistry extends AbstractIdRegistry{

    private static final Logger logger = LoggerFactory.getLogger(SequentialReusableIdRegistry.class);
    /**
     * heartbeat interval (ms), which will be adjusted automatically into the range
     * from 1/20(default) to 1/2 of negotiatedSessionTimeoutMs
     */
    private final int heartbeatIntervalInMs;
    /**
     * Heartbeat executor exponential back off related property. It is a maximum
     * multiplier value for retry delay, in case where a sequence of timeouts occurred.
     */
    private final int expBackOffBound;

    private final HeartbeatHandler heartbeatHandler;

    private final ScheduledExecutorService scheduler;

    private final ThreadPoolExecutor heartbeatExecutor;

    public static class Builder {
        private final CuratorFramework client;
        private final CuratorFramework lockClient;
        private String topic = DefaultTopicEnum.SEQUENTIAL_REUSABLE_ID_REGISTRY.getNodeName();
        private long nextAvailableIdLockTimeoutInMs;
        private int heartbeatIntervalInMs;
        private int expBackOffBound = 10;
        private HeartbeatHandler heartbeatHandler;
        private Supplier<String> customIpSupplier;

        public Builder(CuratorFramework client, CuratorFramework lockClient){
            this.client = client;
            this.lockClient = lockClient;
        }

        public Builder topic(String topic){
            this.topic = topic;
            return this;
        }

        public Builder nextAvailableIdLockTimeoutInMs(long nextAvailableIdLockTimeoutInMs){
            this.nextAvailableIdLockTimeoutInMs = nextAvailableIdLockTimeoutInMs;
            return this;
        }

        public Builder heartbeatIntervalInMs(int heartbeatIntervalInMs){
            if(heartbeatIntervalInMs > 0){
                this.heartbeatIntervalInMs = heartbeatIntervalInMs;
            }
            return this;
        }

        public Builder expBackOffBound(int expBackOffBound){
            if(expBackOffBound > 0){
                this.expBackOffBound = expBackOffBound;
            }
            return this;
        }

        public Builder heartbeatHandler(HeartbeatHandler heartbeatHandler){
            if(Objects.nonNull(heartbeatHandler)){
                this.heartbeatHandler = heartbeatHandler;
            }else {
                this.heartbeatHandler = new HeartbeatHandler();
            }
            return this;
        }

        public Builder customIpSupplier(Supplier<String> customIpSupplier){
            this.customIpSupplier = customIpSupplier;
            return this;
        }

        public SequentialReusableIdRegistry build() throws Exception {
            SequentialReusableIdRegistry registry = new SequentialReusableIdRegistry(this);

            try{
                registry.register();
            }catch (final Exception ex){
                throw new ZktException("Fail to register.", ex);
            }

            registry.startHeartbeat();

            return registry;
        }
    }

    public static Builder builder(CuratorFramework client, CuratorFramework lockClient){
        return new Builder(client, lockClient);
    }

    protected SequentialReusableIdRegistry(Builder builder) throws Exception {
        super(builder.client, builder.lockClient, builder.topic,
                builder.nextAvailableIdLockTimeoutInMs, builder.customIpSupplier);

        final int minHeartbeatIntervalInMs = this.negotiatedSessionTimeoutMs / 20;
        final int maxHeartbeatIntervalInMs = this.negotiatedSessionTimeoutMs / 2;
        if(builder.heartbeatIntervalInMs > 0){
            this.heartbeatIntervalInMs = Math.min(Math.max(builder.heartbeatIntervalInMs, minHeartbeatIntervalInMs), maxHeartbeatIntervalInMs);
        }else{
            this.heartbeatIntervalInMs = minHeartbeatIntervalInMs;
        }

        this.expBackOffBound = builder.expBackOffBound;
        this.heartbeatHandler = builder.heartbeatHandler;

        this.scheduler = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder()
                        .setNameFormat("InstanceIdRegistry-%d")
                        .setDaemon(true)
                        .build());

        this.heartbeatExecutor = new ThreadPoolExecutor(
                1, 2, 0, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadFactoryBuilder()
                        .setNameFormat("InstanceIdRegistry-HeartbeatExecutor-%d")
                        .setDaemon(true)
                        .build()
        );
    }

    public int getHeartbeatIntervalInMs() {
        return this.heartbeatIntervalInMs;
    }

    private String getIpsPath(){
        return ZKPaths.makePath(topic, "ips");
    }

    private String getIdsPath(){
        return ZKPaths.makePath(topic, "ids");
    }

    @Override
    protected void createTopicNode() throws Exception {
        client.create()
                .idempotent()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.CONTAINER).forPath(this.getTopicPath(), null);
    }

    @Override
    protected void registerBeforeProcess(String thisIp) throws Exception {
        client.create().idempotent().withMode(CreateMode.CONTAINER).forPath(this.getIdsPath(), null);
        client.create().idempotent().withMode(CreateMode.CONTAINER).forPath(this.getIpsPath(), null);

        try{
            Integer id = this.getRemoteId(thisIp);
            throw new ZktException(String.format("Fail to register because ip[%s] exists with id[%s]", thisIp, id));
        }catch (KeeperException.NoNodeException ignored){
            super.registerBeforeProcess(thisIp);
        }
    }

    /**
     * Calculate next available id, which will try to reuse the minimum unregistered id first
     *
     * @return next available id
     * @throws Exception exception
     */
    @Override
    public int nextAvailableId() throws Exception {
        final List<Integer> existIds = client.getChildren().forPath(getIdsPath())
                .stream().map(Integer::parseInt).sorted(Comparator.comparingInt(o -> o))
                .collect(Collectors.toList());

        final Optional<Integer> maxId = existIds.stream().max(Comparator.comparingInt(o -> o));

        if(!maxId.isPresent()){
            return 0;
        }

        int nextId = maxId.get() + 1;
        for (int i = 0; i <= maxId.get(); i++) {
            if(i < existIds.get(i)){
                nextId = i;
            }
        }

        return nextId;
    }

    @Override
    protected void registerIpIdNodes(String thisIp, int id) throws Exception {
        CuratorOp ipOp = client.transactionOp().create().withMode(CreateMode.EPHEMERAL)
                .forPath(ZKPaths.makePath(getIpsPath(), thisIp), String.valueOf(id).getBytes(StandardCharsets.UTF_8));

        CuratorOp idOp = client.transactionOp().create().withMode(CreateMode.EPHEMERAL)
                .forPath(ZKPaths.makePath(getIdsPath(), String.valueOf(id)), thisIp.getBytes(StandardCharsets.UTF_8));

        client.transaction().forOperations(ipOp, idOp);
    }

    private Integer getRemoteId(String ip) throws Exception {
        byte[] data = client.getData().forPath(ZKPaths.makePath(getIpsPath(), ip));
        return Integer.parseInt(new String(data, StandardCharsets.UTF_8));
    }

    private String getRemoteIp(int id) throws Exception {
        byte[] data = client.getData().forPath(ZKPaths.makePath(getIdsPath(), String.valueOf(id)));
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public void shutdown() throws Exception {
        this.cancelHeartbeat();
        super.shutdown();
    }

    @Override
    protected void unregister() throws Exception {
        String thisIp = this.getThisIp();

        try{
            int ipNodeId = this.getRemoteId(thisIp);
            try{
                String idNodeIp = this.getRemoteIp(ipNodeId);
                if(thisIp.equals(idNodeIp)){
                    // case 1: normal remove
                    this.removeIpIdNodes(thisIp, Lists.newArrayList(ipNodeId));
                }else{
                    // case 2: expected id is occupied by other ip

                    // then check whether this ip is bound to other ids
                    List<Integer> boundIds = this.locateIdNode(thisIp);
                    this.removeIpIdNodes(thisIp, boundIds);
                }
            }catch (KeeperException.NoNodeException ex){
                // case 3: ip node exist but id node is missing
                this.removeIpIdNodes(thisIp, Collections.emptyList());
            }
        }catch (KeeperException.NoNodeException ex){
            // case 4: ip node is missing, id node is unknown

            // then check whether this ip is bound to other ids
            List<Integer> boundIds = this.locateIdNode(thisIp);
            this.removeIpIdNodes(null, boundIds);
        }

        logger.info("{}[{}] is unregistered", this.getClass().getSimpleName(), this.getGlobalId().orElse(null));
    }

    private List<Integer> locateIdNode(String ip) {
        List<Integer> resultList = Lists.newArrayList();
        String ns = client.getNamespace();

        try{
            for (List<String> idSubList : Lists.partition(client.getChildren().forPath(this.getIdsPath()), 100)) {
                List<Op> opList = idSubList.stream().map(id -> {
                            if(Objects.nonNull(ns) && ns.trim().length() > 0){
                                return Op.getData(ZKPaths.makePath(ns, this.getIdsPath(), id));
                            }else{
                                return Op.getData(ZKPaths.makePath(this.getIdsPath(), id));
                            }
                        }).collect(Collectors.toList());

                int i = 0;
                for (OpResult result : client.getZookeeperClient().getZooKeeper().multi(opList)) {
                    if(result instanceof OpResult.GetDataResult){
                        OpResult.GetDataResult rs = (OpResult.GetDataResult) result;
                        if(ip.equals(new String(rs.getData(), StandardCharsets.UTF_8))){
                            resultList.add(Integer.parseInt(idSubList.get(i)));
                        }
                    }
                    i++;
                }
            }
        }catch (InterruptedException e) {
            logger.error("locateIdNode err. ip:{}", ip, e);
            Thread.currentThread().interrupt();
        }catch (Exception ex){
            throw new ZktException(ex);
        }

        return resultList;
    }

    private void removeIpIdNodes(String ip, List<Integer> ids){
        try {
            List<CuratorOp> opList = Lists.newArrayList();
            if(Objects.nonNull(ip)){
                opList.add(client.transactionOp().delete().forPath(ZKPaths.makePath(this.getIpsPath(), ip)));
            }

            for (Integer id : ids) {
                opList.add(client.transactionOp().delete().forPath(ZKPaths.makePath(this.getIdsPath(), String.valueOf(id))));
            }

            if(opList.isEmpty()){
                return;
            }

            client.transaction().forOperations(opList);
        } catch (final Exception e) {
            throw new ZktException(e);
        }
    }

    protected void startHeartbeat(){
        this.scheduler.schedule(
                new TimedSupervisorTask(
                        this.scheduler,
                        this.heartbeatExecutor,
                        this.heartbeatIntervalInMs,
                        TimeUnit.MILLISECONDS,
                        this.expBackOffBound,
                        new HeartbeatThread()
                ),
                this.heartbeatIntervalInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * The heartbeat task that triggered in the given intervals.
     */
    private class HeartbeatThread implements Runnable {
        @Override
        public void run() {
            logger.info("instance[{}] is sending heartbeat...", SequentialReusableIdRegistry.this.globalId);

            String thisIp = SequentialReusableIdRegistry.this.getThisIp();
            String ns = client.getNamespace();
            List<OpResult> opResults;
            try {
                Op idOp = Op.getData(ZKPaths.makePath(ns, SequentialReusableIdRegistry.this.getIdsPath(), String.valueOf(globalId)));
                Op ipOp = Op.getData(ZKPaths.makePath(ns, SequentialReusableIdRegistry.this.getIpsPath(), thisIp));
                opResults = client.getZookeeperClient().getZooKeeper().multi(ImmutableList.of(idOp, ipOp));
            } catch (InterruptedException e) {
                // case: cannot connect to ZK
                Thread.currentThread().interrupt();
                this.handleConnectionFailed(e);
                return;
            } catch (Exception e) {
                // case: cannot connect to ZK
                this.handleConnectionFailed(e);
                return;
            }

            OpResult idNodeResult = opResults.get(0);
            OpResult ipNodeResult = opResults.get(1);
            if(idNodeResult instanceof OpResult.ErrorResult && ipNodeResult instanceof OpResult.ErrorResult){
                // case: both idNode and ipNode are missing
                try {
                    logger.info("idNode and ipNode are missing， try to recreate...[{}][{}]", thisIp, globalId);
                    SequentialReusableIdRegistry.this.registerIpIdNodes(thisIp, globalId);
                } catch (Exception e) {
                    throw new ZktException(String.format("cannot recreate idNode and ipNode. id:%s, ip:%s", globalId, thisIp), e);
                }
            }else if(idNodeResult instanceof OpResult.ErrorResult){
                // case: idNode is missing
                try {
                    String idNodePath = ZKPaths.makePath(SequentialReusableIdRegistry.this.getIdsPath(), String.valueOf(globalId));
                    logger.info("idNode[{}] is missing, try to recreate with ip[{}]...", idNodePath, thisIp);
                    client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL)
                            .forPath(idNodePath, thisIp.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    throw new ZktException(String.format("cannot recreate idNode. id:%s, ip:%s", globalId, thisIp), e);
                }
            }else if(ipNodeResult instanceof OpResult.ErrorResult){
                // case: ipNode is missing
                try {
                    String ipNodePath = ZKPaths.makePath(SequentialReusableIdRegistry.this.getIpsPath(), thisIp);
                    logger.info("ipNode[{}] is missing, try to recreate with id[{}]...", ipNodePath, globalId);
                    client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL)
                            .forPath(ipNodePath, String.valueOf(globalId).getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    throw new ZktException(String.format("cannot recreate ipNode. id:%s, ip:%s", globalId, thisIp), e);
                }
            }else {
                // both idNode and ipNode exist
                this.checkRemoteId(ipNodeResult);
                this.checkRemoteIp(idNodeResult);
            }
        }

        private void checkRemoteIp(OpResult idNodeResult){
            String remoteIp = new String(((OpResult.GetDataResult) idNodeResult).getData(), StandardCharsets.UTF_8);

            if(!Objects.equals(remoteIp, SequentialReusableIdRegistry.this.getThisIp())){
                // remoteId is occupied by other ip
                this.handleGlobalIdOccupied(SequentialReusableIdRegistry.this.globalId, remoteIp);
            }
        }

        private void checkRemoteId(OpResult ipNodeResult){
            int remoteId = Integer.parseInt(new String(((OpResult.GetDataResult) ipNodeResult).getData(), StandardCharsets.UTF_8));
            if(globalId != remoteId){
                // assigned globalId != remoteId, this should not occur
                throw new ZktException(String.format("globalId and remoteId are not the same for ip: %s, pls check.", SequentialReusableIdRegistry.this.getThisIp()));
            }
        }

        private void handleConnectionFailed(Exception ex){
            if(heartbeatHandler.isHandlingOnConnectionFailed.compareAndSet(false, true)){
                SequentialReusableIdRegistry.this.heartbeatHandler.onConnectionFailed(ex);
                heartbeatHandler.isHandlingOnConnectionFailed.set(false);
            }
        }

        private void handleGlobalIdOccupied(int globalId, String ipOccupying){
            if(heartbeatHandler.isHandlingOnGlobalIdOccupied.compareAndSet(false, true)){
                SequentialReusableIdRegistry.this.heartbeatHandler.onGlobalIdOccupied(globalId, ipOccupying);
                heartbeatHandler.isHandlingOnGlobalIdOccupied.set(false);
            }
        }
    }

    private void cancelHeartbeat() {
        if (this.heartbeatExecutor != null) {
            this.heartbeatExecutor.shutdownNow();
        }

        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
        }

        logger.info("heartbeat is cancelled.");
    }

    @Override
    public void checkAfterAllReady(int expectedInstanceCount) throws Exception {
        super.checkAfterAllReady(expectedInstanceCount);

        long ipsCount = client.getChildren().forPath(this.getIpsPath()).stream().distinct().count();
        long idsCount = client.getChildren().forPath(this.getIdsPath()).stream().distinct().count();

        if(expectedInstanceCount != ipsCount || expectedInstanceCount != idsCount){
            throw new ZktException(String.format("checkAfterAllReady fail. expectedInstanceCount:%s, ipsCount:%s, idsCount:%s",
                    expectedInstanceCount, ipsCount, idsCount));
        }
    }

    /**
     * Handler for heartbeat actions
     */
    public static class HeartbeatHandler {

        private final AtomicBoolean isHandlingOnConnectionFailed = new AtomicBoolean(false);

        private final AtomicBoolean isHandlingOnGlobalIdOccupied = new AtomicBoolean(false);

        /**
         * Callback method when heartbeat find it failed to connect zk
         * @param ex exception
         */
        protected void onConnectionFailed(Exception ex){
            throw new ZktException("HeartbeatThread connection error, try to reconnect...", ex);
        }

        /**
         * Callback method when heartbeat find the globalId has been occupied by other ip
         * @param globalId globalId that being occupied
         * @param occupiedByIp instance ip which is occupying the globalId
         */
        protected void onGlobalIdOccupied(int globalId, String occupiedByIp) {
            throw new ZktException(String.format("HeartbeatThread globalId[%s] is occupied by %s", globalId, occupiedByIp));
        }
    }
}
