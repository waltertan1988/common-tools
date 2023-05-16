package com.walter.dsm;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * @author walter.tan
 * @date 2022-09-26 20:07
 */
public abstract class AbstractStateMachine {

    protected final Map<String, Pair<Integer, AbstractStateHandler>> configMap = new HashMap<>();

    /**
     * 初始化状态机数据
     */
    public abstract void init();

    /**
     * 执行对应的handler
     * @param req
     * @return
     */
    public Object handle(StateMachineReq req) {
        int currentStatus = req.getCurrentStatus();
        int eventCode = req.getEventCode();
        return getConfig(currentStatus, eventCode).getRight().handle(req);
    }

    /**
     * 获取流转状态
     * @param currentStatus 当前状态值
     * @param event 事件枚举值
     * @return
     */
    public Integer getAfterStatus(Integer currentStatus, Integer event) {
        return getConfig(currentStatus, event).getLeft();
    }

    /**
     * 获取key
     * @param status  当前状态值
     * @param event  事件枚举值
     * @return
     */
    protected static String getKey(Integer status, Integer event) {
        return status + "-" + event;
    }

    /**
     * 获取一条状态配置记录
     * @param currentStatus
     * @param event
     * @return
     */
    private Pair<Integer, AbstractStateHandler> getConfig(Integer currentStatus, Integer event){
        Pair<Integer, AbstractStateHandler> config = configMap.get(getKey(currentStatus, event));
        if(null == config) {
            String msg = String.format("No config find for currentState: %s, eventCode: %s", currentStatus, event);
            throw new RuntimeException(msg);
        }
        return config;
    }
}
