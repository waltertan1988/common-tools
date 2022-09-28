package com.walter.orchestration.test.activity.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tyx
 * @date 2021/6/29
 *
 */
@Data
@Accessors(chain = true)
public class DemoActivityChainReq {

    private long id;

    private long uid;
}
