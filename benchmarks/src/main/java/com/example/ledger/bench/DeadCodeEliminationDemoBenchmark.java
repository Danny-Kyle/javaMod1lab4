package com.example.ledger.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Lab steps 3-4: a deliberately wrong benchmark, then two ways to fix it.
 * Not one of the three "real methods" — this is the standalone demonstration
 * of dead code elimination the lab asks for.
 *
 * wrongDiscardsResult() computes a sum but never uses it. The JIT can prove
 * the result is unobserved and is free to delete the entire loop, so this
 * benchmark measures approximately nothing and reports a suspiciously fast,
 * meaningless number.
 *
 * fixedReturnsResult() and fixedConsumesWithBlackhole() are the two standard
 * fixes: return the value so the harness consumes it, or hand it to a
 * Blackhole explicitly. Either stops the JIT from eliminating the work.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
@State(Scope.Benchmark)
public class DeadCodeEliminationDemoBenchmark {

    private int[] data;

    @Setup
    public void setup() {
        data = new int[10_000];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
    }

    @Benchmark
    public void wrongDiscardsResult() {
        int sum = 0;
        for (int value : data) {
            sum += value * value;
        }
        // sum is never read again -> the JIT is free to remove this entire loop.
    }

    @Benchmark
    public int fixedReturnsResult() {
        int sum = 0;
        for (int value : data) {
            sum += value * value;
        }
        return sum;
    }

    @Benchmark
    public void fixedConsumesWithBlackhole(Blackhole blackhole) {
        int sum = 0;
        for (int value : data) {
            sum += value * value;
        }
        blackhole.consume(sum);
    }
}
