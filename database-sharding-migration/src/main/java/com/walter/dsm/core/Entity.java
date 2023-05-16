package com.walter.dsm.core;

import java.lang.annotation.*;

/**
 * @author walter.tan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Entity {
    /**
     * 数据库名称
     * @return
     */
    String database();

    /**
     * 数据表名称
     * @return
     */
    String table();
}
