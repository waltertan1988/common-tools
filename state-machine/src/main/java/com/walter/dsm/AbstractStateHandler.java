package com.walter.dsm;

/**
 * @author walter.tan
 * @date 2022-09-26 20:08
 */
public abstract class AbstractStateHandler {

    private final AbstractStateMachine stateMachine;

    protected AbstractStateHandler(AbstractStateMachine stateMachine){
        this.stateMachine = stateMachine;
    }

    public Object handle(StateMachineReq req) {
        this.before(req);
        Integer afterStatus = stateMachine.getAfterStatus(req.getCurrentStatus(), req.getEventCode());
        Object result = this.doHandler(req, afterStatus);
        this.after(req, afterStatus, result);
        return result;
    }

    /**
     * 前置处理
     * @param req
     */
    public void before(StateMachineReq req) {

    }

    /**
     * 具体业务逻辑处理，实现该方法
     * @param req
     * @param afterStatus
     * @return
     */
    public abstract Object doHandler(StateMachineReq req, Integer afterStatus);

    /**
     * 后置处理
     * @param req
     * @param afterStatus
     * @param object
     */
    public void after(StateMachineReq req, Integer afterStatus, Object object) {

    }
}
