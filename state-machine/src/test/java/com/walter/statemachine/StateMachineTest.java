package com.walter.statemachine;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author walter.tan
 * @date 2022-09-26 20:36
 */
public class StateMachineTest {

    private AbstractStateMachine stateMachine;

    @Before
    public void before(){
        stateMachine = new TestStateMachine();
        stateMachine.init();
    }

    @Test
    public void handle(){
        StateMachineReq req = new StateMachineReq();
        req.setCurrentStatus(0);
        req.setEventCode(0);
        req.setParam("hello");
        Object result = stateMachine.handle(req);
        Assert.assertTrue((Boolean) result);
    }

    private static class TestStateMachine extends AbstractStateMachine {
        @Override
        public void init() {
            this.configMap.put(getKey(0, 0), ImmutablePair.of(1, new TestStateHandler00(this)));
        }
    }

    private static class TestStateHandler00 extends AbstractStateHandler {
        protected TestStateHandler00(AbstractStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public void before(StateMachineReq req) {
            System.out.println("[before] currentStatus: " + req.getCurrentStatus());
            System.out.println("[before] eventCode: " + req.getEventCode());
            System.out.println("[before] param: " + req.getParam());
        }

        @Override
        public Object doHandler(StateMachineReq req, Integer afterStatus) {
            System.out.println("[doHandler] afterStatus: " + afterStatus);
            return true;
        }

        @Override
        public void after(StateMachineReq req, Integer afterStatus, Object result) {
            System.out.println("[after] result: " + result);
        }
    }
}
