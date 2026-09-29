package com.example.ledger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Simulated downstream client: a blocking remote call that returns the fee table.
 * The latency is configurable (ledger.downstream.latency-ms, default 200 ms) so the
 * load profile is I/O bound, which is where virtual threads make a difference.
 */
@Component
public class FeeTableClient {

    private final long latencyMs;

    public FeeTableClient(@Value("${ledger.downstream.latency-ms:200}") long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Map<String, BigDecimal> fetchTable() {
        try {
            Thread.sleep(Duration.ofMillis(latencyMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching the fee table", e);
        }
        BigDecimal rate = new BigDecimal("0.031");
        return Map.of("GBP", rate, "EUR", rate, "USD", rate);
    }
}
