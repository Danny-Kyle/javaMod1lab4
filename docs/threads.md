# Virtual Thread Migration and Pinning Hunt

> Fill every `___` from YOUR own runs. Nothing in this file is pre-measured.

## 1. Toolchain
- `java -version`: ___java 25.0.4 2026-07-21 LTS
- `mvn -v`: ___Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
- Branch: `feature/virtual-threads`

## 2. Current pooling (lab step 3)
| Executor | Core size | Max size | Queue capacity |
|---|---|---|---|
| `applicationExecutor` (ThreadPoolTaskExecutor, `ExecutorConfig`) | 32 | 64 | 500 |

Tomcat's own request pool (defaults, for reference): max threads ___, accept-count ___.

## 3. Load profile
`k6 run load\settlement.js` — 200 VUs, 60 s, `GET /payments/settlement?merchantId=MR-4471`,
downstream latency `___` ms, service restarted before each run.

## 4. Before / after (warm-path migration, steps 2 and 5)
| Run | Throughput (req/s) | p50 | p95 | p99 | Errors |
|---|---|---|---|---|---|
| Baseline (platform threads) | ___ | ___ | ___ | ___ | ___ |
| Virtual threads enabled | ___ | ___ | ___ | ___ | ___ |

Verdict (improved / flat / worse — no explanation yet): ___

## 5. Planted pinning defect (steps 6-8)
Cold-start run with the defect: throughput ___, p50 ___, p95 ___, p99 ___

### One captured `jdk.VirtualThreadPinned` event
```
(paste one event from: jfr print --events jdk.VirtualThreadPinned pinned.jfr)
```
- Duration: ___
- Pinning reason reported: ___
- Frame that cannot unmount: ___
- Call that blocks inside it: ___

### Why "replace synchronized with ReentrantLock" is old advice, and why it buys nothing here
(Two sentences, in your own words.)
1. ___
2. ___

## 6. Fix (step 9)
What changed: ___
Why result semantics are identical: ___
Concurrency test: `FeeTableTest.tableIsLoadedOnceAndSafeUnderConcurrentFirstUse`

## 7. Final before / after (cold start)
| Run | Throughput (req/s) | p50 | p95 | p99 | Pinned events |
|---|---|---|---|---|---|
| Planted defect (`pinned.jfr`) | ___ | ___ | ___ | ___ | ___ |
| Fixed (`fixed.jfr`) | ___ | ___ | ___ | ___ | 0 |

## 8. Pull request
- **Change:** ___
- **Risk:** ___
- **Rollback:** ___
