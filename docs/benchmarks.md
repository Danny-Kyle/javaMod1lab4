# JMH Benchmark Suite


## 1. Methods chosen (lab step 2)
| Method | Why chosen |
|---|---|
| `PaymentController.toResponse(PaymentEntity)` | Runs on every POST /payments response; a serialisation/mapping method. |
| `ProblemMessages.joinFieldErrors(List<String>)` | Runs on every invalid POST /payments; string/collection-heavy work on the error path. |
| `SettlementService.computeOwed(long, BigDecimal)` | The core settlement calculation; stays on `long` minor units and `BigDecimal` rather than `double` because binary floating point cannot hold a money value exactly. |

(All three are real production methods, extracted as small static functions so they
can be measured without a database or Spring context — see main-patch/README.md
for exactly what was extracted and why.)

## 2. Dead code elimination demonstration (lab steps 3-4)
Benchmark: `DeadCodeEliminationDemoBenchmark`

| Variant | Score | Error | Unit |
|---|---|---|---|
| `wrongDiscardsResult` (broken — result discarded) | 0.001 | ± 0.009 | us/op |
| `fixedReturnsResult` (fixed — value returned) | 0.739 | ± 0.254 | us/op |
| `fixedConsumesWithBlackhole` (fixed — Blackhole) | 0.754 | ± 0.238 | us/op |

Size of the dead-code-elimination error: the broken version reported 750×
faster (or: reported a time indistinguishable from measurement noise / near
zero) than either fixed version, because the JIT proved `sum` was never read
and deleted the loop entirely.

## 3. Run configuration (lab step 6)
`@BenchmarkMode(Mode.AverageTime)`, `@OutputTimeUnit(TimeUnit.MICROSECONDS)`,
`@Warmup(iterations = 5)`, `@Measurement(iterations = 10)`, `@Fork(3)`.

Why forks matter (one line, your own words): Forks run benchmarks in separate JVM processes to prevent JIT compilation profile pollution and account for OS-level runtime variance

## 4. Results table (lab steps 7-9) — from `results.json`
| Benchmark | Score | Error | Unit | Bytes/op (`gc.alloc.rate.norm`) |
|---|---|---|---|---|
| `MappingBenchmark.mapEntityToResponse` | 0.009 | ± 0.001 | us/op | 32.000  0.001 B/op |
| `StringJoinBenchmark.joinFieldErrorMessages` | 0.310 | ± 0.080 | us/op | 501.334 10.251 B/op |
| `SettlementCalcBenchmark.computeSettlementOwed` | 0.022 | ± 0.006 | us/op | ~~10?? B/op |

Comparisons: None of the error bars overlap confirming each benchmark measures a distinct, statistically significant performance difference.

## 5. Limits of this suite (lab step 10 — name at least three)
1. No Concurrency / Load: It measures isolated, single-threaded method execution and reveals nothing about system behavior under thread contention or parallel HTTP traffic.

2. Fixed Synthetic Data: It uses small, static inputs in memory, failing to reflect cache misses or allocation behavior seen with real production data sizes.
3. Isolated from Framework Stack: It executes in a microbenchmark harness without real-world runtime overhead like Tomcat request handling, Jackson JSON serialization, or database/JPA persistence.
4. ___ (optional extra limit)
