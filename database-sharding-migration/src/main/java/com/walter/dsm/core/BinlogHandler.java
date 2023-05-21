package com.walter.dsm.core;

/**
 * @author walter.tan
 */
public interface BinlogHandler {

    /**
     * 支持的数据库名称
     * @return
     */
    String supportDatabaseName();

    /**
     * 支持的数据表名称
     * @return
     */
    String supportTableName();
}
