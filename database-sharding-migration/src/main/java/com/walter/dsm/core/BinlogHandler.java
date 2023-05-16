package com.walter.dsm.core;

import com.alibaba.otter.canal.protocol.CanalEntry;

/**
 * @author walter.tan
 */
public interface BinlogHandler {

    /**
     * 处理一批binlog变更记录
     * @param rowChage
     */
    void handler(CanalEntry.RowChange rowChage);
}
