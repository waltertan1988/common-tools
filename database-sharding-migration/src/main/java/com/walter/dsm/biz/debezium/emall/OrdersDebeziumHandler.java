package com.walter.dsm.biz.debezium.emall;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.ImmutableMap;
import com.walter.dsm.core.debezium.BinlogDebeziumHandler;
import io.debezium.data.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * @author walter.tan
 * @date 2023-05-16 23:21
 */
@Slf4j
@Component
public class OrdersDebeziumHandler implements BinlogDebeziumHandler {

    @Override
    public String supportDatabaseName() {
        return "el_shop_emall";
    }

    @Override
    public String supportTableName() {
        return "orders";
    }

    @Override
    public void handler(Optional<Map<String, Object>> before, Optional<Map<String, Object>> after, Envelope.Operation op) {
        if (Envelope.Operation.DELETE == op) {
            System.out.println(JSON.toJSONString(before.orElse(null)));
        } else if (Envelope.Operation.CREATE == op) {
            System.out.println(JSON.toJSONString(after.orElse(null)));
        } else if (Envelope.Operation.UPDATE == op) {
            System.out.println(JSON.toJSONString(ImmutableMap.of("before", before.orElse(null), "after", after.orElse(null))));
        }
    }
}
