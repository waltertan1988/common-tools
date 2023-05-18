package com.walter.dsm.core.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.walter.dsm.core.AbstractCdcProcessor;
import com.walter.dsm.core.CdcType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author walter.tan
 * @date 2023-05-16 21:56
 */
@Slf4j
@Component
public class CanalProcessor extends AbstractCdcProcessor {
    @Value("${app.canal.server.ip}")
    private String canalServerIp;

    @Value("${app.canal.server.port:11111}")
    private int canalServerPort;

    private final Map<String, BinlogHandler> binlogHandlerMap;

    private static final int batchSize = 1000;

    public CanalProcessor(@Autowired List<BinlogHandler> binlogHandlerList){
        Map<String, BinlogHandler> tempMap = new HashMap<>();

        for (BinlogHandler handler : binlogHandlerList) {
            tempMap.put(this.getBinlogHandlerKey(handler.supportDatabaseName(), handler.supportTableName()), handler);
        }

        binlogHandlerMap = Collections.unmodifiableMap(tempMap);
    }

    @Override
    public CdcType supportCdcType() {
        return CdcType.CANAL;
    }

    @Override
    public void run() {
        // 创建链接
        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress(canalServerIp,
                canalServerPort), "example", "", "");

        try {
            connector.connect();
            connector.subscribe(".*\\..*");
            connector.rollback();
            while (super.isRunning) {
                Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    this.handleEntries(message.getEntries());
                }

                connector.ack(batchId); // 提交确认
                // connector.rollback(batchId); // 处理失败, 回滚数据
            }

            log.info("coreThread is stopped.");
        } finally {
            connector.disconnect();
        }
    }

    private String getBinlogHandlerKey(String database, String table){
        return database + "." + table;
    }

    private void handleEntries(List<CanalEntry.Entry> entries){
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }

            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(), e);
            }

            String handlerKey = this.getBinlogHandlerKey(entry.getHeader().getSchemaName(), entry.getHeader().getTableName());
            BinlogHandler binlogHandler = binlogHandlerMap.get(handlerKey);
            Assert.notNull(binlogHandler, "cannot find BinlogHandler");
            binlogHandler.handler(rowChange);
        }
    }
}
