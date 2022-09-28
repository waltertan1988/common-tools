package com.walter.orchestration.test.pay.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tyx
 * @date 2021/6/29
 *
 */
@Data
@Accessors(chain = true)
public class DemoPayVipChainReq {

    private long uid;

    private long amount;
}
