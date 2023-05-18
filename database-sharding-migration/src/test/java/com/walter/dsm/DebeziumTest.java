package com.walter.dsm;

import io.debezium.config.Configuration;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.engine.DebeziumEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author walter.tan
 * @date 2023-05-18 21:23
 */
@Slf4j
public class DebeziumTest extends BaseTests{

    @Test
    public void embeddedServerTest() throws InterruptedException {
        Configuration config = Configuration.create()
                /* begin engine properties */
                .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", "C:/work/temp/debezium/offset.dat")
                .with("offset.flush.interval.ms", 60000)
                /* begin connector properties */
                .with("name", "my-sql-connector")
                .with("database.hostname", "192.168.10.14")
                .with("database.port", 3306)
                .with("database.user", "root")
                .with("database.password", "123456")
                .with("database.server.id", 85744)
                .with("database.server.name", "my-app-connector")
                .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
                .with("database.history.file.filename", "C:/work/temp/debezium/dbhistory.dat")
                .build();

        // Create the engine with this configuration ...
        EmbeddedEngine engine = EmbeddedEngine.create()
                .using(config)
                .notifying(this::handleEvent)
                .build();

        // Run the engine asynchronously ...
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);

        TimeUnit.MINUTES.sleep(60);

        // At some later time ...
        engine.stop();

        try {
            while (!engine.await(30, TimeUnit.SECONDS)) {
                log.info("Wating another 30 seconds for the embedded engine to shut down");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleEvent(List<SourceRecord> records, DebeziumEngine.RecordCommitter<SourceRecord> committer) {
        System.out.println(records);
        try {
            committer.markBatchFinished();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
