package com.walter.dsm.core.debezium;

import com.google.common.collect.Maps;
import com.walter.dsm.core.AbstractCdcProcessor;
import com.walter.dsm.core.CdcType;
import io.debezium.config.Configuration;
import io.debezium.data.Envelope;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author walter.tan
 * @date 2023-05-18 15:08
 */
@Slf4j
@Component
public class DebeziumProcessor extends AbstractCdcProcessor {

    @Value("${app.debezium.database.host}")
    private String debeziumDatabaseHost;

    @Value("${app.debezium.database.port}")
    private String debeziumDatabasePort;

    @Value("${app.debezium.database.user}")
    private String debeziumDatabaseUser;

    @Value("${app.debezium.database.password}")
    private String debeziumDatabasePassword;

    @Value("${app.debezium.database.serverId}")
    private String debeziumDatabaseServerId;

    @Value("${app.debezium.data.dir}")
    private String debeziumDataDir;

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private ExecutorService executor;

    private final Map<String, BinlogDebeziumHandler> binlogDebeziumHandlerMap;

    public DebeziumProcessor(@Autowired List<BinlogDebeziumHandler> binlogDebeziumHandlerList){
        Map<String, BinlogDebeziumHandler> tempMap = new HashMap<>();

        for (BinlogDebeziumHandler handler : binlogDebeziumHandlerList) {
            tempMap.put(this.getBinlogHandlerKey(handler.supportDatabaseName(), handler.supportTableName()), handler);
        }

        binlogDebeziumHandlerMap = Collections.unmodifiableMap(tempMap);
    }

    @Override
    public CdcType supportCdcType() {
        return CdcType.DEBEZIUM;
    }

    @Override
    public void run() {
        /*
         * 参考资料：
         *  https://debezium.io/documentation/reference/1.4/operations/embedded.html
         *  https://debezium.io/documentation/reference/1.4/development/engine.html
         */
        Configuration config = Configuration.create()
                /* begin engine properties */
                .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", debeziumDataDir + "/offset.dat")
                .with("offset.flush.interval.ms", 60000)
                /* begin connector properties */
                .with("name", "my-sql-connector")
                .with("database.hostname", debeziumDatabaseHost)
                .with("database.port", debeziumDatabasePort)
                .with("database.user", debeziumDatabaseUser)
                .with("database.password", debeziumDatabasePassword)
                .with("database.server.id", debeziumDatabaseServerId)
                // database.server.name就是connector的topic前缀
                .with("database.server.name", "my-app-connector")
                .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
                .with("database.history.file.filename", debeziumDataDir + "/dbhistory.dat")
                .build();

        // Create the engine with this configuration ...
        engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying(this::handleBatch)
                .build();

        // Run the engine asynchronously ...
        executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);
    }

    private void handleBatch(List<RecordChangeEvent<SourceRecord>> events, DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> committer) {
        for (RecordChangeEvent<SourceRecord> event : events) {
            try {
                SourceRecord sourceRecord = event.record();
                Struct sourceRecordValue = (Struct) sourceRecord.value();
                if(Objects.isNull(sourceRecordValue)){
                    continue;
                }

                // before
                Struct before = (Struct) sourceRecordValue.get(Envelope.FieldName.BEFORE);
                final Optional<Map<String, Object>> beforeMap = Optional.ofNullable(Objects.isNull(before) ? null : Maps.newHashMap());
                beforeMap.ifPresent(m -> {
                    for (Field field : before.schema().fields()) {
                        Optional.ofNullable(before.get(field)).ifPresent(v -> m.put(field.name(), v));
                    }
                });

                // after
                Struct after = (Struct) sourceRecordValue.get(Envelope.FieldName.AFTER);
                final Optional<Map<String, Object>> afterMap = Optional.ofNullable(Objects.isNull(after) ? null : Maps.newHashMap());
                afterMap.ifPresent(m -> {
                    for (Field field : after.schema().fields()) {
                        Optional.ofNullable(after.get(field)).ifPresent(v -> m.put(field.name(), v));
                    }
                });

                // source
                Struct source = (Struct) sourceRecordValue.get(Envelope.FieldName.SOURCE);
                String db = source.getString("db");
                String table = source.getString("table");

                // op
                Envelope.Operation operation = Envelope.operationFor(sourceRecord);

                String handlerKey = this.getBinlogHandlerKey(db, table);
                BinlogDebeziumHandler binlogDebeziumHandler = binlogDebeziumHandlerMap.get(handlerKey);
                Assert.notNull(binlogDebeziumHandler, "cannot find BinlogDebeziumHandler");
                binlogDebeziumHandler.handler(beforeMap, afterMap, operation);

                committer.markProcessed(event);
            } catch (InterruptedException e) {
                log.error("handleBatch event fail", e);
            } catch (Exception e){
                log.error("handleBatch event error", e);
            }
        }

        try {
            committer.markBatchFinished();
        } catch (InterruptedException e) {
            log.error("handleBatch fail", e);
        }
    }

    @Override
    public void stop() {
        try {
            engine.close();
            executor.shutdown();
            while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.info("Waiting another 5 seconds for the embedded engine to shut down");
            }
        } catch (IOException | InterruptedException e) {
            log.error("stop error.", e);
            executor.shutdownNow();
        }

        super.stop();
    }
}
