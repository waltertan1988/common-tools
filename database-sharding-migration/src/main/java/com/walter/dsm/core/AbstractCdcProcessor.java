package com.walter.dsm.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;

/**
 * @author walter.tan
 */
@Slf4j
public abstract class AbstractCdcProcessor implements Runnable {

    @Getter
    protected volatile boolean isRunning;

    /**
     * 支持的CDC引擎类型
     * @return
     */
    public abstract CdcType supportCdcType();

    public void start(){
        if(isRunning){
            return;
        }

        isRunning = true;

        new Thread(this).start();

        log.info("{} cdc thread is running.", this.getClass().getName());
    }

    public void stop(){
        isRunning = false;
    }

    @PreDestroy
    public void preDestroy(){
        this.stop();
    }

    protected String getBinlogHandlerKey(String database, String table){
        return database + "." + table;
    }
}
