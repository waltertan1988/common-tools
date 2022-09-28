package com.walter.orchestration.test.pay.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tyx
 * @date 2021/7/2
 *
 */
@Data
@Accessors(chain = true)
public class DemoPayVipChainRes {

    private boolean isVip;
    private boolean hasGift;
}
