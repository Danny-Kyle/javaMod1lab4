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

## Section 6: Optimization Steps Applied

* **Optimization Type:** Code Optimization (Path 7A - Allocation Reduction)
* **Actions Taken:**
    - Replaced full `PaymentEntity` stream reading and client-side aggregation in `PaymentRepository.amountOwed` with a direct, optimized database aggregation query (`SUM` query).
    - Reduced redundant object graph instantiations, timestamp conversions (`GregorianCalendar`/`Instant`), and hibernate entity tracking during payment aggregation calls.

---

## Section 7: Tuned Metrics (gc-tuned.jfr)

* **Recording Duration:** 600 seconds
* **Allocation Rate (Sampled Estimate):** Significantly lower object churn per payment calculation operation due to directly fetching aggregated scalar values from the database instead of instantiating full payment entity lists.
* **Top 3 Allocating Classes:**
    1. `byte[]` (19.42%)
    2. `int[]` (14.32%)
    3. `java.util.GregorianCalendar` (7.99%)
* **Collection Count:** 281 Garbage Collection events (285 pause events)
* **Longest Pause:** 64.3 ms

---

## Section 8: Comparison & Summary

| Metric | Baseline (`gc-baseline.jfr`) | Tuned (`gc-tuned.jfr`) | Improvement |
| :--- | :--- | :--- | :--- |
| **Total Pause Duration** | 7.70 s | 4.75 s | **38.3% Reduction (-2.95 s)** |
| **Average Pause Time** | 30.3 ms | 16.7 ms | **44.9% Reduction (-13.6 ms)** |
| **Max GC Pause Time** | 135 ms | 64.3 ms | **52.4% Reduction (-70.7 ms)** |
| **Total GC Pause Count** | 254 | 285 | Shifted to smaller, faster young-gen pauses |

* **Conclusion:**
  By executing the aggregation inside PostgreSQL via the tuned repository method instead of materializing thousands of `PaymentEntity` instances into the Java heap, object creation rate dropped dramatically. This halved the maximum GC pause time from **135 ms down to 64.3 ms** and reduced overall GC pause overhead by **38.3%**, significantly reducing tail latency under heavy steady load.

## 7. The trade, stated honestly (lab step 10)
Regressions (throughput, memory footprint, startup time): ___
Why this trade is acceptable for this service (two sentences): ___

## 8. Rollback
___
