package com.example.ledger.bench;

import com.example.ledger.ProblemMessages;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Real method #2: the string/collection-heavy method. Chosen because it's on
 * the error path for every invalid POST /payments request (ProblemHandler),
 * and string joining over a growing field-error list is an easy place to hide
 * needless allocation. The input list is built once in @Setup so its size and
 * contents can't be constant-folded away.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
@State(Scope.Benchmark)
public class StringJoinBenchmark {

    private List<String> fieldMessages;

    @Setup
    public void setup() {
        fieldMessages = List.of(
                "merchantId must not be blank",
                "amountMinor must be greater than 0",
                "currency must not be blank",
                "currency size must be between 3 and 3",
                "merchantId size must be between 1 and 255");
    }

    @Benchmark
    public String joinFieldErrorMessages() {
        return ProblemMessages.joinFieldErrors(fieldMessages);
    }
}
