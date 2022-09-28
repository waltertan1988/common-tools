package com.walter.concurrent.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author walter.tan
 */
@Getter
@AllArgsConstructor
public enum ThreadQueueType {
    SynchronousQueueWithFair("1"),
    SynchronousQueue("2"),
    LinkedBlockingQueue("3"),
    LinkedBlockingQueueWithQueueSize("4")
    ;

    private String value;
}
