package com.example.ledger.bench;

import com.example.ledger.PaymentController;
import com.example.ledger.PaymentEntity;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Real method #1: the serialisation/mapping method. Chosen because it runs on
 * every POST /payments response and is the kind of small, easily-overlooked
 * per-request allocation that adds up under load (see the GC lab).
 * Inputs live in @State and are built once in @Setup, so the JIT cannot fold
 * the entity's fields into compile-time constants.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
@State(Scope.Benchmark)
public class MappingBenchmark {

    private PaymentEntity entity;

    @Setup
    public void setup() {
        entity = new PaymentEntity(
                "11111111-1111-1111-1111-111111111111",
                "MR-4471",
                128_450L,
                "GBP",
                Instant.now());
    }

    @Benchmark
    public PaymentController.PaymentResponse mapEntityToResponse() {
        return PaymentController.toResponse(entity);
    }
}
