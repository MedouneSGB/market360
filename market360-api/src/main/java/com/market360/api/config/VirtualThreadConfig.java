package com.market360.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Active les virtual threads pour tous les handlers HTTP Tomcat.
 */
@Configuration
public class VirtualThreadConfig {

    @Bean
    public Executor applicationTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
