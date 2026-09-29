# Garbage Collection Under Load

> Fill every `___` from YOUR own runs. Nothing here is pre-measured.

## 1. Starting point (lab step 1)
Output of `java -XX:+PrintFlagsFinal -version` filtered for the two flags:

size_t MaxHeapSize                              = 6413090816                                {product}
{ergonomic}

size_t SoftMaxHeapSize                          = 6413090816                             {manageable}
{ergonomic}

bool UseG1GC                                  = true                                      {product}
{ergonomic}

java version "25.0.4" 2026-07-21 LTS

Java(TM) SE Runtime Environment (build 25.0.4+7-LTS-189)

Java HotSpot(TM) 64-Bit Server VM (build 25.0.4+7-LTS-189, mixed mode, sharing)


- `MaxHeapSize` = 6116 MB
- `UseG1GC` = true (true means G1 is the default collector on this machine)
- `java -version`: 25.04
- Machine: ___ CPU cores, ___ GB RAM

## 2. Load profile (lab step 2)
- Script: `perf/steady-load.js`, executor `constant-arrival-rate`, rate 10 req/s, duration 10m
- Endpoint: `GET /payments/settlement?merchantId=MR-9001`
- Dataset: 20,000 payment rows for MR-9001 (`perf/seed.sql`)
- Answer returned (must be identical in both runs): INFO[0000] SETUP answer for MR-9001: {"merchantId":"MR-9001","amountOwedMinor":10581674}  source=console
- Why this rate: 10 req/s (held with `dropped_iterations` = 0)

## 3. Baseline numbers (lab step 4) — `gc-baseline.jfr`
| Measure | Value |
|---|---|
| Total allocation rate (MB/s) | ___ |
| Top 3 allocating classes | 1. 20.05%  2. 13.91%  3. 7.86% |
| Collection count | 249 garbage collection events |
| Longest pause | 135 ms |
| Where the numbers came from | (JMC page / `jfr view` command) ___ |

-Note: The 10-minute recording clock starts when the JVM starts, a little before k6, so the recording ends ~20-30 s before k6 does

## 4. Classification — written BEFORE changing anything (lab step 5)
Classification: Allocation Pressure

Evidence:
The profile is heavily dominated by object allocations stemming from database tuple decoding (org.postgresql.core.PGStream.receiveTupleV3 at 17.84% and Calendar / Date creation at >28% combined). The top classes being byte[], int[], and GregorianCalendar show that Hibernate is constantly deserializing massive lists of database rows into heap objects during payment processing.

Prescribed Fix: Code Change (Path 7A) — Reduce allocations at the site by replacing full entity/payment loading with a database-level SUM query.

## 5. The one change (lab steps 6 or 7)
- Type: code fix  /  heap size  /  collector   (delete the ones that do not apply)
- Hot allocation site found (class.method, from the recording): ___
- Diff or exact flag string used: ___
- Everything else identical to baseline: ___

## 6. Comparison (lab step 9) — `gc-baseline.jfr` vs `gc-tuned.jfr`
| Measure | Baseline | Tuned | Change |
|---|---|---|---|
| Allocation rate (MB/s) | ___ | ___ | ___ |
| Collection count | ___ | ___ | ___ |
| Longest pause (ms) | ___ | ___ | ___ |
| p99 request latency (ms, k6) | ___ | ___ | ___ |
| Throughput (req/s, k6 `http_reqs` rate) | ___ | ___ | ___ |

## 7. The trade, stated honestly (lab step 10)
Regressions (throughput, memory footprint, startup time): ___
Why this trade is acceptable for this service (two sentences): ___

## 8. Rollback
___
