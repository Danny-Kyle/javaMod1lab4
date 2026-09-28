package com.example.ledger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The application executor that serves GET /payments/settlement.
 * Write these three numbers into docs/threads.md (lab step 3):
 *   core = 32, max = 64, queue capacity = 500.
 * Note: a ThreadPoolExecutor only grows past core when the queue is full,
 * so with 200 users and a 500-slot queue this pool never leaves 32 threads.
 */
@Configuration
public class ExecutorConfig {

    @Bean(name = "applicationExecutor")
    public ThreadPoolTaskExecutor applicationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(32);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ledger-app-");
        return executor;
    }
}
