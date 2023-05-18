package com.walter.dsm.biz.canal.emall;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.walter.dsm.core.canal.BinlogHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author walter.tan
 * @date 2023-05-16 23:21
 */
@Slf4j
@Component
public class OrdersHandler implements BinlogHandler {

    @Override
    public String supportDatabaseName() {
        return "el_shop_emall";
    }

    @Override
    public String supportTableName() {
        return "orders";
    }

    @Override
    public void handler(CanalEntry.RowChange rowChange) {
        CanalEntry.EventType eventType = rowChange.getEventType();

        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            if (eventType == CanalEntry.EventType.DELETE) {
                printColumn(rowData.getBeforeColumnsList());
            } else if (eventType == CanalEntry.EventType.INSERT) {
                printColumn(rowData.getAfterColumnsList());
            } else {
                System.out.println("-------> before");
                printColumn(rowData.getBeforeColumnsList());
                System.out.println("-------> after");
                printColumn(rowData.getAfterColumnsList());
            }
        }
    }

    private void printColumn(List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
        }
    }
}
