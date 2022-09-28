package com.walter.zkt.enums;

import com.walter.zkt.core.registry.SequentialReusableIdRegistry;
import com.walter.zkt.core.registry.SequentialUniqueIdRegistry;

/**
 * Default topic name
 *
 * @author walter.tan
 * @date 2022-09-07 9:21
 */
public enum DefaultTopicEnum {

    /**
     * Topic name for function {@link SequentialReusableIdRegistry}
     */
    SEQUENTIAL_REUSABLE_ID_REGISTRY("default_sequential_reusable_id_registry"),
    /**
     * Topic name for function {@link SequentialUniqueIdRegistry}
     */
    SEQUENTIAL_UNIQUE_ID_REGISTRY("default_sequential_unique_id_registry");

    /**
     * node name in zookeeper
     */
    private final String nodeName;

    DefaultTopicEnum(String nodeName){
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }
}
