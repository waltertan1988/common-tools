package com.walter.orchestration.test.gift;

import com.walter.orchestration.base.AbstractChainNode;
import com.walter.orchestration.base.ChainContext;
import com.walter.orchestration.test.gift.bo.DemoVipGiftReq;
import com.walter.orchestration.test.gift.bo.DemoVipGiftRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * vip用户，获得小礼品
 * @author tyx
 * @date 2021/6/29
 *
 */
@Slf4j
@Component
public class DemoVipGiftNode extends AbstractChainNode<DemoVipGiftReq, DemoVipGiftRes> {

    @Override
    public DemoVipGiftRes handle(ChainContext context, DemoVipGiftReq req) {
        // 在这里编写本节点的业务逻辑
        DemoVipGiftRes res = new DemoVipGiftRes().setHasGift(req.isVip());
        // 把处理结果记录到上下文里
        log.info(">>>>>> DemoChainVipGiftNode.execute, req={}, res:{}", req, res);
        return res;
    }
}
