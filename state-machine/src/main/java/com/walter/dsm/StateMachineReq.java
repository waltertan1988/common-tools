package com.walter.dsm;

/**
 * @author walter.tan
 * @date 2022-09-26 20:10
 */
public class StateMachineReq {
    /**
     * 当前状态
     */
    private Integer currentStatus;
    /**
     * 事件编码
     */
    private Integer eventCode;
    /**
     * 业务对象
     */
    private Object param;

    public Integer getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(Integer currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Integer getEventCode() {
        return eventCode;
    }

    public void setEventCode(Integer eventCode) {
        this.eventCode = eventCode;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }
}
