package com.walter.orchestration.test.activity.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tyx
 * @date 2021/7/2
 *
 */
@Data
@Accessors(chain = true)
public class DemoActivityChainRes {

    private boolean isVip;
    private boolean hasGift;
}
