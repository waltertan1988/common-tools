package com.walter.orchestration.test.pay;

import com.walter.orchestration.base.*;
import com.walter.orchestration.test.common.DemoChainContextKey;
import com.walter.orchestration.test.gift.DemoVipGiftNode;
import com.walter.orchestration.test.gift.bo.DemoVipGiftReq;
import com.walter.orchestration.test.gift.bo.DemoVipGiftRes;
import com.walter.orchestration.test.pay.bo.DemoPayVipChainReq;
import com.walter.orchestration.test.pay.bo.DemoPayVipChainRes;
import com.walter.orchestration.util.CastUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 购买vip获得vip资格，vip可获得小礼品
 * @author tyx
 * @date 2021/6/29
 *
 */
@Slf4j
@Component
public class DemoPayVipChain extends AbstractChain {
    @Autowired
    @ChainOrder(1) // 定义链节点的执行顺序（声明方式实现）
    private DemoPayNode demoPayNode;
    @Autowired
    @ChainOrder(2)
    private DemoVipGiftNode demoVipGiftNode; // 可复用的业务节点

    @Override
    protected void registChainNodeParamResolver(Map<Class<? extends AbstractChainNode>, NodeParamResolver> chainNodeParamResolverMap) {
        // 对每个链节点的入参添加NodeParamResolver，用于给各自的入参进行初始化
        chainNodeParamResolverMap.put(DemoPayNode.class, context -> {
            DemoPayVipChainReq demoPayVipChainReq = CastUtil.cast(context.get(DemoChainContextKey.DemoPayVipChain.REQ));

            DemoPayNode.DemoPayNodeReq req = demoPayNode.new DemoPayNodeReq();
            req.setUid(demoPayVipChainReq.getUid()).setAmount(demoPayVipChainReq.getAmount());
            context.put(demoPayNode.getRequestContextKey(), req);
        });

        chainNodeParamResolverMap.put(DemoVipGiftNode.class, context -> {
            DemoPayVipChainReq demoPayVipChainReq = CastUtil.cast(context.get(DemoChainContextKey.DemoPayVipChain.REQ));
            DemoPayNode.DemoPayNodeRes demoPayNodeRes = CastUtil.cast(context.get(demoPayNode.getResponseContextKey()));

            DemoVipGiftReq req = new DemoVipGiftReq();
            CastUtil.cast(context.get(demoVipGiftNode.getRequestContextKey()));
            req.setUid(demoPayVipChainReq.getUid()).setVip(demoPayNodeRes.isVip());
            context.put(demoVipGiftNode.getRequestContextKey(), req);
        });
    }

    @Override
    protected void preProcessChain(final ChainContext context) {
        log.info("{} preProcessChain() executing...", this.getClass().getSimpleName());
    }

    @Override
    public void postProcessChain(ChainContext context) {
        DemoPayNode.DemoPayNodeRes demoPayNodeRes = CastUtil.cast(context.get(demoPayNode.getResponseContextKey()));
        DemoVipGiftRes demoVipGiftRes = CastUtil.cast(context.get(demoVipGiftNode.getResponseContextKey()));

        DemoPayVipChainRes res = new DemoPayVipChainRes()
                .setVip(demoPayNodeRes.isVip())
                .setHasGift(demoVipGiftRes.isHasGift());
        context.put(DemoChainContextKey.DemoPayVipChain.RES, res);
    }

    /**
     * 以【内部类】方式定义链节点，实现：购买vip
     * @author tyx
     * @date 2021/6/29
     *
     */
    @Component
    public class DemoPayNode extends AbstractChainNode<DemoPayNode.DemoPayNodeReq, DemoPayNode.DemoPayNodeRes> {

        @Override
        public DemoPayNodeRes handle(ChainContext context, DemoPayNodeReq req) {
            DemoPayNodeRes res = new DemoPayNodeRes()
                    .setVip(req.getAmount() > 0);
            log.info(">>>>>> DemoPayNode.execute, req={}, res:{}", req, res);
            return res;
        }

        @Data
        @Accessors(chain = true)
        public class DemoPayNodeReq {
            private long uid;
            private long amount;
        }

        @Data
        @Accessors(chain = true)
        public class DemoPayNodeRes {
            private boolean isVip;
        }
    }
}
