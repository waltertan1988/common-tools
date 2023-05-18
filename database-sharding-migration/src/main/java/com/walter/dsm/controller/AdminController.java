package com.walter.dsm.controller;

import com.alibaba.fastjson2.JSON;
import com.walter.dsm.core.AbstractCdcProcessor;
import com.walter.dsm.core.CdcType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author walter.tan
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Value("${app.onboot.cdcType}")
    private String onBootCdcType;

    private final Map<String, AbstractCdcProcessor> cdcProcessorMap;

    public AdminController(@Autowired List<AbstractCdcProcessor> cdcProcessorList){
        Map<String, AbstractCdcProcessor> tempMap = new HashMap<>();
        for (AbstractCdcProcessor p : cdcProcessorList) {
            tempMap.put(p.supportCdcType().name(), p);
        }
        cdcProcessorMap = Collections.unmodifiableMap(tempMap);

        // 自启动CDC服务线程
        Optional.ofNullable(cdcProcessorMap.get(onBootCdcType)).ifPresent(AbstractCdcProcessor::start);
    }

    /**
     * 测试服务是否正常启动
     * @return
     */
    @GetMapping("/ping")
    public String ping(){
        return "success";
    }

    /**
     * 启动CDC服务
     * @param cdcType CDC引擎类型，参考{@link CdcType}
     * @return
     */
    @GetMapping("/start/{cdcType}")
    public String start(@PathVariable("cdcType") String cdcType){
        AbstractCdcProcessor processor = cdcProcessorMap.get(cdcType);
        if(Objects.isNull(processor)){
            return "cdcType not exist: " + cdcType;
        }
        processor.start();
        return "success";
    }

    /**
     * 停止CDC服务
     * @param cdcType CDC引擎类型，参考{@link CdcType}
     * @return
     */
    @GetMapping("/stop/{cdcType}")
    public String stop(@PathVariable("cdcType") String cdcType){
        AbstractCdcProcessor processor = cdcProcessorMap.get(cdcType);
        if(Objects.isNull(processor)){
            return "cdcType not exist: " + cdcType;
        }
        processor.stop();
        return "success";
    }

    /**
     * 返回所有CDC服务线程的运行状态
     * @return
     */
    @GetMapping("/listAllCdcStatus")
    public String listAllCdcStatus(){
        Map<String, Boolean> statusMap = cdcProcessorMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().isRunning()));
        return JSON.toJSONString(statusMap);
    }
}
