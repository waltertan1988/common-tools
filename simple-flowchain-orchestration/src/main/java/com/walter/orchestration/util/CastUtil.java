package com.walter.orchestration.util;

import lombok.experimental.UtilityClass;

/**
 * @author tyx
 * @date 2021/6/28
 *
 */
@UtilityClass
public class CastUtil {
    public static <T> T cast(Object obj) {
        return (T) obj;
    }
}
