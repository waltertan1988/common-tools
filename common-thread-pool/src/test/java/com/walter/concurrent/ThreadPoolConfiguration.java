package com.walter.concurrent;

import com.walter.concurrent.core.ConfigureContext;
import com.walter.concurrent.core.DefaultConfigureContext;
import com.walter.concurrent.core.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.concurrent.TimeUnit;

/**
 * 通用线程池配置
 * @author walter.tan
 */
@Slf4j
@Configuration
public class ThreadPoolConfiguration implements DisposableBean {

    private ThreadPoolFactory threadPoolFactory;

    @Bean("defaultConfigureContext")
    public ConfigureContext configureContext(){
        return new DefaultConfigureContext();
    }

    @Bean
    @Lazy
    public ThreadPoolFactory threadPoolFactory(@Qualifier("defaultConfigureContext") ConfigureContext context){
        this.threadPoolFactory = new ThreadPoolFactory(context);
        return threadPoolFactory;
    }

    @Override
    public void destroy() {
        threadPoolFactory.shutdownAll(60, TimeUnit.SECONDS);
    }
}
