package com.walter.orchestration.test.common;

/**
 * @author tyx
 * @date 2021/6/29
 *
 */
public interface DemoChainContextKey {

    interface DemoActivityChain {
        String REQ = "REQ";
        String PRE_PROCESS_CHAIN_RES = "PRE_PROCESS_CHAIN_RES";
        String RES = "RES";
    }

    interface DemoPayVipChain {
        String REQ = "REQ";
        String RES = "RES";
    }
}
