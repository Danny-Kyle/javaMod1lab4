package com.example.ledger.bench;

import com.example.ledger.SettlementService;
import org.openjdk.jmh.annotations.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Real method #3, required by the lab: the settlement calculation itself.
 * Chosen because it's the core business rule of the service and it
 * deliberately stays on long minor units and BigDecimal rather than double,
 * since binary floating point cannot represent every money value exactly.
 * totalMinor mirrors the GC lab's seeded total for MR-9001 (20,000 payments)
 * so the input is realistic rather than a round number the JIT might treat
 * specially.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
@State(Scope.Benchmark)
public class SettlementCalcBenchmark {

    private long totalMinor;
    private BigDecimal feeRate;

    @Setup
    public void setup() {
        totalMinor = 10_920_200L;
        feeRate = new BigDecimal("0.031");
    }

    @Benchmark
    public long computeSettlementOwed() {
        return SettlementService.computeOwed(totalMinor, feeRate);
    }
}
