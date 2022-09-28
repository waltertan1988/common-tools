package com.walter.zkt.basic;

import com.walter.zkt.AbstractTest;
import org.apache.zookeeper.CreateMode;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author walter.tan
 * @date 2022-08-26 13:57
 */
@Ignore
public class CuratorTest extends AbstractTest {

    @Test
    public void create() throws Exception {
        String result = client.create()
                .idempotent()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath("/pulsar/topic1", "4".getBytes(StandardCharsets.UTF_8));
        print("create result:" + result);
    }

    @Test
    public void set() throws Exception {
        client.setData().forPath("/pulsar/topic1", "8".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void get() throws Exception {
        byte[] bytes = client.getData().forPath("/pulsar/topic1");
        print("get result:" + new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void getChildren() throws Exception {
        // 不使用namespace，即从根节点出发
        List<String> children1 = client.usingNamespace(null).getChildren().forPath("/");
        print("getChildren result1:" + children1);

        List<String> children2 = client.getChildren().forPath("/");
        print("getChildren result2:" + children2);
    }

    @Test
    public void delete() throws Exception {
        client.delete()
                .guaranteed()
                .deletingChildrenIfNeeded()
                .forPath("/pulsar");
    }
}
