# JMH Benchmark Suite

## Methods and service paths

| Method | Why measured |
|---|---|
| `PaymentController.toResponse(PaymentEntity)` | Maps every successful POST response; creates the response record. |
| `ProblemMessages.joinFieldErrors(List<String>)` | Builds validation-error detail on the invalid POST path. |
| `SettlementService.computeOwed(long, BigDecimal)` | Applies the core fee rule on the settlement read path, preserving long minor units, BigDecimal and RoundingMode.DOWN. |

Production methods remain in the service; wrappers, state and JMH dependencies
live in `benchmarks`. The mapping state uses a payment shaped like the service
request, the join state uses field-error messages, and the calculation state
uses the seed total and fee rate. Inputs are created in `@Setup` under
`@State(Scope.Benchmark)`, outside measured methods.

## Reproduce

Run `powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\run-benchmarks.ps1` from the repository root.
See [README](../README.md) for prerequisites, build-only checks and the exact
commands. No manual local-Maven install or external sibling folder is required.

## Configuration

All four classes use AverageTime, MICROSECONDS, five warmup iterations,
ten measurement iterations and three forks. The historical JSON records ten
seconds per iteration. New runs retain the per-iteration console output so
warmup stability can be assessed; do not assert stability without checking it.

Student draft retained for confirmation, not replaced by agent-authored prose:

Forks run benchmarks in separate JVM processes to prevent JIT compilation profile pollution and account for OS-level runtime variance

## Historical three-method result

Source: [`results.json`](../results.json), recovered byte-for-byte from the
original sibling module. It records Java 25.0.4+7-LTS-189 and JMH 1.37. This is
existing evidence, not a fresh run of the integrated reactor. The new runner
will save a source commit, jar hash and machine manifest for its next run.

| Benchmark | Time score +/- error | Allocated bytes/op +/- error |
|---|---|---|
| `MappingBenchmark.mapEntityToResponse` | 0.00902785 +/- 0.00114297 us/op | 32.000006 +/- 8.01907e-07 B/op |
| `SettlementCalcBenchmark.computeSettlementOwed` | 0.0221702 +/- 0.00551283 us/op | 1.5446873e-05 +/- 3.82067e-06 B/op |
| `StringJoinBenchmark.joinFieldErrorMessages` | 0.310268 +/- 0.0800445 us/op | 501.33355 +/- 10.2507 B/op |

All scores, errors and units above come from the matching primaryMetric and
secondaryMetrics.gc.alloc.rate.norm entries in results.json. The allocation
near zero for the settlement method is retained as measured; it must not be
replaced with a guessed object size. The JIT can remove intermediate allocations.

None of these three recorded score intervals overlap. They represent different
operations, so this observation does not demonstrate that an implementation
change improved performance. No optimization win is claimed from comparing
mapping, string joining and fee calculation. Comparable variants must be judged
by their own intervals; overlapping comparisons must be labelled inconclusive.
The confidence level for the final report must be checked against the saved JMH
console output; it is not explicitly labelled in this historical JSON.

For allocation-site interpretation, mapping constructs the response record in
PaymentController.toResponse; joining invokes Collectors.joining in
ProblemMessages.joinFieldErrors. These are source-level candidate allocation
sites, not a replacement for an allocation profiler's stack evidence.

## Dead-code-elimination demonstration

`DeadCodeEliminationDemoBenchmark` keeps the deliberately discarded result,
return-value fix and Blackhole fix in source. The result of each fixed method
is observable to JMH. The discarded calculation allows the JIT to remove work.

Previous report figures were 0.001 +/- 0.009 us/op (discarded),
0.739 +/- 0.254 us/op (return), and 0.754 +/- 0.238 us/op (Blackhole).
No raw demo output was found, so these remain unverified historical claims and
are not final evidence. No precise 750x speedup is asserted: the discarded
score's interval covers zero and makes a ratio unstable. The two reported fixed
intervals overlap; that historical comparison is inconclusive, pending raw
results. Run the script and use dce-results.json/dce.txt for the final table.

## Limits — student draft retained for confirmation

1. No Concurrency / Load: It measures isolated, single-threaded method execution and reveals nothing about system behavior under thread contention or parallel HTTP traffic.

2. Fixed Synthetic Data: It uses small, static inputs in memory, failing to reflect cache misses or allocation behavior seen with real production data sizes.
3. Isolated from Framework Stack: It executes in a microbenchmark harness without real-world runtime overhead like Tomcat request handling, Jackson JSON serialization, or database/JPA persistence.

For Exceeds, the student should add a specific further measurement addressing
each limit. The forks sentence and final limits must remain the student's words.

## Evidence still needed before resubmission

- A clean Java 25 build/test log for this source and a newly identified run.
- Raw DCE scores/errors, the exact command and any overrides (none planned).
- A verified JMH confidence level, warmup assessment and elapsed full-run time.
- Student confirmation/revision of the forks sentence and three limits.

See [evidence index](../evidence/README.md). Historical results remain clearly
separate until the student supplies the new run; missing results are not invented.
