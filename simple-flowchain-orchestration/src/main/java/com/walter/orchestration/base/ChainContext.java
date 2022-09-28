package com.walter.orchestration.base;

import java.util.HashMap;
import java.util.Map;

/**
 * @author tyx
 * @date 2021/6/28
 *
 */
public class ChainContext {

    private Map<String, Object> ctxMap = new HashMap<>();

    public Object put(String key, Object val){
        return ctxMap.put(key, val);
    }

    public Object get(String key){
        return ctxMap.get(key);
    }
}
