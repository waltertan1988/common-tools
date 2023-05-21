package com.walter.dsm.core.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.walter.dsm.core.BinlogHandler;

/**
 * @author walter.tan
 */
public interface BinlogCanalHandler extends BinlogHandler {

    /**
     * 处理一批binlog变更记录
     * @param rowChage
     */
    void handler(CanalEntry.RowChange rowChage);
}
