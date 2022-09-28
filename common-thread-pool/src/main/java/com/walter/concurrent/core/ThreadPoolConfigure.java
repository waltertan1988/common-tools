package com.walter.concurrent.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * @author walter.tan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ThreadPoolConfigure {
    /**
     * 线程池的Key
     */
    private String key;
    /**
     * 队列类型，参考{@link ThreadQueueType}
     */
    private String type;
    private int corePoolSize;
    private int maxPoolSize;
    private long keepAliveTime;
    /**
     * 线程执行的超时时间, 小于等于0表示不超时
     */
    private long timeout;
    /**
     * 等待队列的公平策略
     */
    private Boolean fair;
    /**
     * 等待队列的长度
     */
    private int initQueueSize;
    /**
     * 等待队列超过此值，则打印队列长度
     */
    private int showThreadQueueSize;

    private ThreadFactory threadFactory;

    private RejectedExecutionHandler rejectedExecutionHandler;
}
