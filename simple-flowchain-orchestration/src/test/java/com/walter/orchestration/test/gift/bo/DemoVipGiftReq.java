package com.walter.orchestration.test.gift.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tyx
 * @date 2021/6/29
 *
 */
@Data
@Accessors(chain = true)
public class DemoVipGiftReq {

    private long uid;
    private boolean isVip;
}
