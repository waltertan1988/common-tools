package com.walter.dsm.core.debezium;

import com.walter.dsm.core.BinlogHandler;
import io.debezium.data.Envelope;

import java.util.Map;
import java.util.Optional;

/**
 * @author walter.tan
 */
public interface BinlogDebeziumHandler extends BinlogHandler {

    /**
     * 处理一批binlog变更记录
     * @param before 变更前的数据快照
     * @param after 变更后的数据快照
     * @param op 变更操作
     */
    void handler(Optional<Map<String, Object>> before, Optional<Map<String, Object>> after, Envelope.Operation op);
}
