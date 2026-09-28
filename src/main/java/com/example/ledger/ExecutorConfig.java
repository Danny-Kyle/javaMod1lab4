package com.example.ledger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lab step 4: the pooled executor is replaced by one virtual thread per task. */
@Configuration
public class ExecutorConfig {

    @Bean(name = "applicationExecutor")
    public ExecutorService applicationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
