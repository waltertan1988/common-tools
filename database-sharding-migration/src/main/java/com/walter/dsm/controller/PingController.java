package com.walter.dsm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author walter.tan
 */
@Slf4j
@RestController
@RequestMapping("/ping")
public class PingController {
    
    /**
     * 测试服务是否正常启动
     * @return
     */
    @GetMapping
    public String ping(){
        return "success";
    }
}
