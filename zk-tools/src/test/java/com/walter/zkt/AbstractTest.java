package com.walter.zkt;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author walter.tan
 * @date 2022-08-26 17:16
 */
public abstract class AbstractTest {

    protected static final String CONN_STR = "127.0.0.1:2181";

    protected static final String NAME_SPACE = "app";

    protected static CuratorFramework client;

    @BeforeClass
    public static void beforeClass(){
        client = newCuratorFramework();
        info("client is started.");
    }

    @AfterClass
    public static void afterClass(){
        CloseableUtils.closeQuietly(client);
        info("client is closed successfully.");
    }

    public static CuratorFramework newCuratorFramework(){
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(CONN_STR)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(NAME_SPACE)
                .build();

        client.start();

        return client;
    }

    protected static void info(final String msg){
        System.out.println("[INFO]" + msg);
    }
    protected static void error(final String msg){
        System.out.println("[ERROR]" + msg);
    }

    protected void print(final Object object){
        System.out.println(">>>>>>" + object);
    }
}
