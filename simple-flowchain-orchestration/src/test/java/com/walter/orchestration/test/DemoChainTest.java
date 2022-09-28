package com.walter.orchestration.test;

import com.walter.orchestration.base.ChainContext;
import com.walter.orchestration.test.activity.DemoActivityChain;
import com.walter.orchestration.test.activity.bo.DemoActivityChainReq;
import com.walter.orchestration.test.activity.bo.DemoActivityChainRes;
import com.walter.orchestration.test.common.BaseTests;
import com.walter.orchestration.test.common.DemoChainContextKey;
import com.walter.orchestration.test.pay.DemoPayVipChain;
import com.walter.orchestration.test.pay.bo.DemoPayVipChainReq;
import com.walter.orchestration.test.pay.bo.DemoPayVipChainRes;
import com.walter.orchestration.util.CastUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author tyx
 * @date 2021/6/29
 *
 */
@Slf4j
public class DemoChainTest extends BaseTests {
    @Autowired
    private DemoActivityChain demoActivityChain;
    @Autowired
    private DemoPayVipChain demoPayVipChain;

    /**
     * 范例 1：前10位参与某活动的用户，获得vip资格，且vip可获得小礼品
     */
    @Test
    public void demoActivityChainTest(){

        // 1. 设置调用链的起始入参到上下文
        ChainContext context = new ChainContext();
        DemoActivityChainReq req = new DemoActivityChainReq().setUid(10086L).setId(1L);
        context.put(DemoChainContextKey.DemoActivityChain.REQ, req);

        // 2. 开始执行调用链
        demoActivityChain.start(context);

        // 3. 从调用链的上下文获取需要的结果
        DemoActivityChainRes res = CastUtil.cast(context.get(DemoChainContextKey.DemoActivityChain.RES));

        // 4. 根据上下文的调用结果，继续处理后续的业务逻辑
        log.info(">>>>>>test end. req:{}, res:{}", req, res);
    }

    /**
     * 范例 2：购买vip获得vip资格，vip可获得小礼品
     */
    @Test
    public void demoPayVipChainTest(){

        // 1. 设置调用链的起始入参到上下文
        ChainContext context = new ChainContext();
        DemoPayVipChainReq req = new DemoPayVipChainReq().setUid(10086L).setAmount(1L);
        context.put(DemoChainContextKey.DemoPayVipChain.REQ, req);

        // 2. 开始执行调用链
        demoPayVipChain.start(context);

        // 3. 从调用链的上下文获取需要的结果
        DemoPayVipChainRes res = CastUtil.cast(context.get(DemoChainContextKey.DemoPayVipChain.RES));

        // 4. 根据上下文的调用结果，继续处理后续的业务逻辑
        log.info(">>>>>>test end. req:{}, res:{}", req, res);
    }
}
