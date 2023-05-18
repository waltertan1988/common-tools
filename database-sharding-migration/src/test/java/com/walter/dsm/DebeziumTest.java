package com.walter.dsm;

import io.debezium.config.Configuration;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.engine.DebeziumEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

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

    @Test
    public void embeddedServerTest() throws InterruptedException {
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
                .with("database.server.name", "my-app-connector")
                .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
                .with("database.history.file.filename", debeziumDataDir + "/dbhistory.dat")
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
