# JMH Benchmark Suite

## Methods and service paths

| Method | Why measured |
|---|---|
| `PaymentController.toResponse(PaymentEntity)` | Maps every successful POST response; repeated response-record construction is a per-request allocation cost. |
| `ProblemMessages.joinFieldErrors(List<String>)` | Builds validation detail on the invalid POST path; measures string/collection work for the service's error responses. |
| `SettlementService.computeOwed(long, BigDecimal)` | Core fee calculation on every settlement read; preserves long minor units, BigDecimal and RoundingMode.DOWN. |

Production methods remain in the service; benchmark wrappers and JMH dependencies
live in `benchmarks`. Inputs are created in `@Setup` under `@State(Scope.Benchmark)`.
The mapping input has the service's payment shape, string state contains field-error
messages, and the settlement state uses the seed total and configured fee rate.
No Spring context or database request is part of a measured method.

## Reproduction and run identity

Run `powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\run-benchmarks.ps1`
from a clean repository root with Java 25, Maven and Docker running. See the
[README](../README.md) for the single-command build and exact JMH invocations.
The reactor builds the service and benchmark module without a manual jar install.

The student ran the complete command on 29 October 2026. The
[manifest](../evidence/jmh-20261029-133647/manifest.json) identifies source commit
`ae8f0d4b4810375aa123526c308ae4318be05135`, machine `OKEY-DAN` with
8 logical processors, Oracle Java 25.0.4+7-LTS-189,
Maven 3.9.16, command arguments and the benchmark-jar SHA-256. The stored jar hash
was checked against the built jar. The manifest records completion and
2710.839470 seconds for both benchmark invocations together,
excluding the preceding build/test phase. This is one observed duration, not a
guarantee for another machine or run.

Both suites use JMH 1.37, AverageTime, MICROSECONDS, five ten-second warmups,
ten ten-second measurements and three forks. Every method has 30 measured scores,
stored as three forks of ten. No warmup/fork reductions or JVM argument overrides
were used. Both console logs identify compiler Blackholes; the mode was consistent.
JMH warns that compiler Blackholes are experimental. Generated machine code was
not inspected, so these observations do not prove the exact optimized instruction
sequence or generalize to another JVM/Blackhole mode.

Why forks matter:

Forks run benchmarks in separate JVM processes to prevent JIT compilation profile pollution and account for OS-level runtime variance

## Three real methods: final run

Source for every score/error/unit below: matching entries in
[`results.json`](../results.json), byte-identical to the
[archived run JSON](../evidence/jmh-20261029-133647/results.json). Allocation comes from
`secondaryMetrics.gc.alloc.rate.norm`; time comes from `primaryMetric`.

| Benchmark | Time score +/- error (us/op) | Allocation score +/- error (B/op) |
|---|---|---|
| `MappingBenchmark.mapEntityToResponse` | 0.0111422159 +/- 0.000602535231 | 32.0000078 +/- 4.19984166e-07 |
| `SettlementCalcBenchmark.computeSettlementOwed` | 0.0218896282 +/- 0.00129250144 | 1.5307792e-05 +/- 9.07156715e-07 |
| `StringJoinBenchmark.joinFieldErrorMessages` | 0.209854552 +/- 0.0126288061 | 490.666813 +/- 10.25074 |

The logs label these score/error intervals as 99.9% confidence intervals and
state the normal-distribution assumption. No interval for the three different
operations overlaps, but that is not a controlled comparison of interchangeable
implementations. It does not establish an optimization win between mapping,
joining and calculation. The near-zero settlement allocation is the measured
profiler result, not a guessed object size; intermediate objects may be optimized
away. A result below one byte/op is an average, not a fractional object.

Mapping constructs the response record in `PaymentController.toResponse`;
the join method uses `Collectors.joining` in `ProblemMessages.joinFieldErrors`.
Those are concrete source-level allocation sites. The aggregate GC profiler does
not supply stack attribution, so the string method's entire allocation total
is not assigned to a single source line without further profiling.

## Deliberately broken benchmark and both fixes

Source for every score/error/unit below:
[`dce-results.json`](../evidence/jmh-20261029-133647/dce-results.json). The kept source has an identical
sum-of-squares loop in all three variants; one discards its result, one returns
it and one consumes it with Blackhole. The discarded loop has no observable
result and is eligible for dead-code elimination; its tiny reported time is
consistent with elimination of useful work. Returning/consuming the result
makes the computation observable to the harness. No assembly-based claim is made.

| Benchmark | Time score +/- error (us/op) | Allocation score +/- error (B/op) |
|---|---|---|
| `DeadCodeEliminationDemoBenchmark.fixedConsumesWithBlackhole` | 0.721244454 +/- 0.111167736 | 0.000504288331 +/- 7.77066212e-05 |
| `DeadCodeEliminationDemoBenchmark.fixedReturnsResult` | 0.649434241 +/- 0.0117840436 | 0.000454210356 +/- 8.21817305e-06 |
| `DeadCodeEliminationDemoBenchmark.wrongDiscardsResult` | 0.000866378407 +/- 4.10393989e-06 | 6.05936352e-07 +/- 2.65278611e-09 |

Point-estimate ratios computed from the unrounded JSON scores are
749.596522 for return/discarded and
832.482029 for Blackhole/discarded; derivation is
`fixed primaryMetric.score / discarded primaryMetric.score` in the
[verification record](../evidence/jmh-20261029-133647/verification.json). Thus the broken harness reports
a time hundreds of times smaller than the observable-work variants. This is a
benchmarking error demonstration, not a service speedup. The exact discarded
interval is positive; the rounded console display must not be interpreted as
an interval reaching zero. The JSON is authoritative for the precision above.

**Return versus Blackhole is inconclusive.** Their 99.9% intervals are
[0.637650197, 0.661218284] us/op (return) and
[0.610076718, 0.83241219] us/op (Blackhole), taken directly from
`primaryMetric.scoreConfidence` in dce-results.json. They overlap. No claim is made
that either fixed approach is faster. Return is convenient for a single result;
Blackhole can consume results explicitly when a return value alone is unsuitable.

## Warmup review and statistical honesty

The full warmup and measurement lines are retained in
[`suite.txt`](../evidence/jmh-20261029-133647/suite.txt) and [`dce.txt`](../evidence/jmh-20261029-133647/dce.txt).
The mapping fork warmups are broadly similar, but stability is not proved for
every method. For example, settlement fork 3 warmup iterations 3–5 report
0.018, 0.041 and 0.028 us/op in suite.txt; Blackhole fork 3 iterations 4–5 report
0.640 and 0.823 us/op in dce.txt. These are observed variations, not discarded
outliers. Five warmups were kept as specified, and no claim is made that they
establish sufficient convergence for every case. Any stronger warmup conclusion
would require further controlled measurements.

The earlier report incorrectly treated differences between unrelated methods
as statistically significant optimization results. That interpretation is
withdrawn. This run does not establish that the earlier differences were caused
by noise: hardware/power state and the historical run identity are not sufficiently
controlled for such a causal claim. The two fixed DCE intervals here overlap,
so their apparent point-estimate gap does not justify choosing one for speed.

## Limits

1. No Concurrency / Load: It measures isolated, single-threaded method execution and reveals nothing about system behavior under thread contention or parallel HTTP traffic.

   Follow-up measurement (proposed, not performed): run the settlement HTTP
   endpoint under several fixed concurrency levels with k6, keeping the dataset
   unchanged. Record throughput, p95/p99 latency and errors, and inspect thread
   and database-connection contention with JFR. This tests how the complete
   service scales under concurrent requests.

2. Fixed Synthetic Data: It uses small, static inputs in memory, which do not establish cache-miss or allocation behavior at production data sizes.

   Follow-up measurement (proposed, not performed): parameterize the join
   collection and demonstration-array sizes with JMH `@Param`, using a documented
   range of workload-relevant sizes and working sets. Record time/op and B/op
   for each size; collect cache-miss counters where platform support permits.
   Varying size is a useful sensitivity check, not proof that the inputs match
   an actual production workload.

3. Isolated from Framework Stack: It executes in a microbenchmark harness without real-world runtime overhead like Tomcat request handling, Jackson JSON serialization, or database/JPA persistence.

   Follow-up measurement (proposed, not performed): profile the real payment,
   validation-error and settlement HTTP paths with the Spring application and
   Postgres running. Save end-to-end request latency from k6 together with JFR
   execution/allocation samples to identify framework and database work absent
   from these isolated method benchmarks. Do not subtract unrelated timings
   and label the difference as framework overhead.

## Evidence status and remaining submission checks

- Completed raw suite and DCE JSON, raw logs, manifest and verification are archived.
- The root results.json is the new final-run JSON, not the recovered old run.
- Earlier sibling results are preserved separately as historical evidence.
- The student's successful Java 25 build-only transcript is retained; the final
  runner also gates both suites on a successful build/test/discovery pass.
- The forks sentence and original three limits were confirmed as the student's
  writing. The student requested help adding the proposed follow-up measurements
  and reviewing technical wording; those additions should be read and understood
  before submission. No follow-up measurement above is claimed to have run.
- An independent local clone at commit `2ff5699` passed the documented
  `run-benchmarks.ps1 -BuildOnly` command on Java 25, with one payment integration
  test, no failures/errors/skips, and six discovered benchmark methods. The log
  is [clean-clone-build-2026-10-29.txt](../evidence/clean-clone-build-2026-10-29.txt).
  This check used the same Windows machine and existing Maven cache; it is not
  a second-machine or CI measurement, and it did not rerun the benchmarks.
- The student confirmed the laptop was plugged in. Power mode and other
  unrecorded settings are unknown; background activity was not independently
  recorded. See the supplemental run-context file; the raw manifest is unchanged.
- Changes remain local until the complete submission review; no PR was merged.

See [evidence index](../evidence/README.md) for every file and its provenance.
