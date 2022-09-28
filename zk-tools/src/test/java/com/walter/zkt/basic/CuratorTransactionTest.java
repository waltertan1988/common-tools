package com.walter.zkt.basic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.walter.zkt.AbstractTest;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.framework.api.transaction.CuratorTransactionResult;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.OpResult;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author walter.tan
 * @date 2022/9/4
 *
 */
public class CuratorTransactionTest extends AbstractTest {

    @Test
    public void transactionComparison() throws Exception {
        client.create().withMode(CreateMode.CONTAINER).forPath("/test1");
        client.create().withMode(CreateMode.CONTAINER).forPath("/test2");
        client.create().withMode(CreateMode.CONTAINER).forPath("/test3");

        final int times = 10;
        final int batch = 1000;
        final int total = batch * times;

        // 1. execution by looping
        long begin = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/test1/node");
        }
        info("cost1 ms: " + (System.currentTimeMillis() - begin));


        // 2. execution by multi-thread
        begin = System.currentTimeMillis();
        final List<CompletableFuture<?>> completableFutureList = Lists.newArrayList();
        final ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < times; i++) {
            final CompletableFuture<?> cf = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < batch; j++) {
                    try {
                        client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/test2/node");
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, executor);
            completableFutureList.add(cf);
        }
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
        info("cost2 ms: " + (System.currentTimeMillis() - begin));


        // 3. execution by transaction
        begin = System.currentTimeMillis();
        final List<CuratorOp> opList = Lists.newArrayList();
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < batch; j++) {
                final CuratorOp op = client.transactionOp().create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/test3/node");
                opList.add(op);
            }
            client.transaction().forOperations(opList);
            opList.clear();
        }
        info("cost3 ms: " + (System.currentTimeMillis() - begin));


        // assert result
        final int nodeCount1 = client.checkExists().forPath("/test1").getNumChildren();
        final int nodeCount2 = client.checkExists().forPath("/test2").getNumChildren();
        final int nodeCount3 = client.checkExists().forPath("/test3").getNumChildren();
        Assert.assertTrue(nodeCount1 == nodeCount2 && nodeCount1 == nodeCount3);
    }

    @Test
    public void normalTransaction() throws Exception {
        client.delete().idempotent().guaranteed().deletingChildrenIfNeeded().forPath("/test");
        client.create().withMode(CreateMode.CONTAINER).forPath("/test");

        final CuratorOp op1 = client.transactionOp().create().withMode(CreateMode.EPHEMERAL).forPath("/test/node1");
        final CuratorOp op2 = client.transactionOp().create().withMode(CreateMode.EPHEMERAL).forPath("/test/node2");
        for (final CuratorTransactionResult result : client.transaction().forOperations(op1, op2)) {
            info(String.format("path: %s, type: %s, err: %s", result.getForPath(), result.getType(), result.getError()));
        }
        Assert.assertNotNull(client.checkExists().forPath("/test/node1"));
        Assert.assertNotNull(client.checkExists().forPath("/test/node2"));
    }

    @Test
    public void abnormalTransaction() throws Exception {
        client.delete().idempotent().guaranteed().deletingChildrenIfNeeded().forPath("/test");
        client.create().withMode(CreateMode.CONTAINER).forPath("/test");

        client.create().withMode(CreateMode.EPHEMERAL).forPath("/test/node2");
        final CuratorOp op1 = client.transactionOp().create().withMode(CreateMode.EPHEMERAL).forPath("/test/node1");
        final CuratorOp op2 = client.transactionOp().create().withMode(CreateMode.EPHEMERAL).forPath("/test/node2");

        final AtomicBoolean isOk = new AtomicBoolean(false);
        client.transaction().inBackground((client, event) -> {
            info(String.format("callback -> path: %s, type: %s, err: %s", event.getPath(), event.getType(), event.getResultCode()));
            for (final CuratorTransactionResult rs : event.getOpResults()) {
                info(String.format("callback rs -> rsPath:%s, path:%s, type:%s, err:%s", rs.getResultPath(), rs.getForPath(), rs.getType(), rs.getError()));
            }
            isOk.set(true);
        }).withUnhandledErrorListener((msg, e) -> {
            info("other err: " + msg);
            e.printStackTrace();
        }).forOperations(op1, op2);

        while(!isOk.get()){
            Thread.sleep(1000);
        }

        Assert.assertNull(client.checkExists().forPath("/test/node1"));
    }

    @Test
    public void multiRead() throws Exception {
        for (int i = 0; i < 3; i++) {
            client.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath("/test/" + i, ("hello" + i).getBytes(StandardCharsets.UTF_8));
        }

        String ns = client.getNamespace();
        List<OpResult> results = client.getZookeeperClient().getZooKeeper().multi(ImmutableList.of(
                Op.getChildren(ZKPaths.makePath(ns, "/test")),
                Op.getData(ZKPaths.makePath(ns, "/test/3")),
                Op.getData(ZKPaths.makePath(ns, "/test/0")),
                Op.getData(ZKPaths.makePath(ns, "/test/1"))));

        KeeperException ex = null;
        for (OpResult result : results) {
            if(result instanceof OpResult.GetChildrenResult){
                OpResult.GetChildrenResult rs = (OpResult.GetChildrenResult) result;
                print(rs.getChildren());
                Assert.assertEquals(3, rs.getChildren().size());
            }else if(result instanceof OpResult.GetDataResult){
                OpResult.GetDataResult rs = (OpResult.GetDataResult) result;
                String data = new String(rs.getData(), StandardCharsets.UTF_8);
                print(data);
                Assert.assertTrue(data.startsWith("hello"));
            }else if(result instanceof OpResult.ErrorResult){
                OpResult.ErrorResult rs = (OpResult.ErrorResult) result;
                print(rs.getErr());
                ex = KeeperException.create(KeeperException.Code.get(rs.getErr()));
            }
        }

        Assert.assertTrue(Objects.nonNull(ex) && ex instanceof KeeperException.NoNodeException);
    }
}
