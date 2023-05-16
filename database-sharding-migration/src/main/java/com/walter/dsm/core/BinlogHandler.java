package com.walter.dsm.core;

import com.alibaba.otter.canal.protocol.CanalEntry;

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

    /**
     * 处理一批binlog变更记录
     * @param rowChage
     */
    void handler(CanalEntry.RowChange rowChage);
}
