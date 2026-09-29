package com.example.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FeeTableTest {

    private static FeeTableClient countingClient(AtomicInteger calls) {
        return new FeeTableClient(0) {
            @Override
            public Map<String, BigDecimal> fetchTable() {
                calls.incrementAndGet();
                return Map.of("GBP", new BigDecimal("0.031"));
            }
        };
    }

    @Test
    void tableIsLoadedOnceAndSafeUnderConcurrentFirstUse() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        FeeTable feeTable = new FeeTable(countingClient(calls));

        int callers = 200;
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<BigDecimal>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < callers; i++) {
                results.add(pool.submit(() -> {
                    startGun.await();
                    return feeTable.rateFor("GBP", BigDecimal.ZERO);
                }));
            }
            startGun.countDown();
            for (Future<BigDecimal> result : results) {
                assertThat(result.get()).isEqualByComparingTo("0.031");
            }
        }

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void unknownCurrencyFallsBackToTheDefaultRate() {
        FeeTable feeTable = new FeeTable(countingClient(new AtomicInteger()));

        assertThat(feeTable.rateFor("JPY", new BigDecimal("0.05"))).isEqualByComparingTo("0.05");
    }
}
