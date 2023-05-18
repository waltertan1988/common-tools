package com.walter.dsm.core.debezium;

import com.walter.dsm.core.AbstractCdcProcessor;
import com.walter.dsm.core.CdcType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author walter.tan
 * @date 2023-05-18 15:08
 */
@Slf4j
@Component
public class DebeziumProcessor extends AbstractCdcProcessor {

    @Override
    public CdcType supportCdcType() {
        return CdcType.DEBEZIUM;
    }

    @Override
    public void run() {
        // 参考https://debezium.io/documentation/reference/1.4/operations/embedded.html


    }
}
