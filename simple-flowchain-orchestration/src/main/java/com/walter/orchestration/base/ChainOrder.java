package com.walter.orchestration.base;

import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * 指定调用链中的节点的执行顺序
 *
 * @author tyx
 * @date 2021/6/28
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface ChainOrder {
    /**
     * 执行顺序（值越小，越靠前执行）
     * @return
     */
    int value() default Ordered.LOWEST_PRECEDENCE;
}
