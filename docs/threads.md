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

Tomcat's own request pool (defaults, for reference): max threads 200, accept-count 100.

## 3. Load profile
`k6 run load\settlement.js` — 200 VUs, 60 s, `GET /payments/settlement?merchantId=MR-4471`,
downstream latency `200` ms, service restarted before each run.

## 4. Before / after (warm-path migration, steps 2 and 5)
| Run | Throughput (req/s) | p50 | p95 | p99 | Errors |
|---|---|---|---|---|---|
| Baseline (platform threads) | 139.88 | 1.36 | 1.71 | 2.25 | 0.00% |
| Virtual threads enabled | 842.63 | 214.8 | 324.0 | 461.3 | 0.15% |

Verdict: improved throughput (about 6x) and lower p50, p95 and p99; 78 requests were refused during the run.

## 5. Planted pinning defect (steps 6-8)
Cold-start run with the defect: throughput 856.99 ms, p50 189.86ms, p95 467.54ms, p99 831.32ms

### One captured `jdk.VirtualThreadPinned` event
```

  jdk.VirtualThreadPinned {
    startTime = 18:45:13.266 (2026-09-28)
    duration = 223 ms
    blockingOperation = "LockSupport.park"
>   pinnedReason = "VM call to com.example.ledger.FeeTableHolder.<clinit> on stack"
    carrierThread = "ForkJoinPool-1-worker-1" (javaThreadId = 57)
    eventThread = "" (javaThreadId = 342, virtual)
    stackTrace = [
      java.lang.VirtualThread.parkOnCarrierThread(boolean, long) line: 851
      java.lang.VirtualThread.parkNanos(long) line: 819
      java.lang.VirtualThread.sleepNanos(long) line: 1001
      java.lang.Thread.sleepNanos(long) line: 549
      java.lang.Thread.sleep(Duration) line: 643
      com.example.ledger.FeeTableClient.fetchTable() line: 26
>     com.example.ledger.FeeTableHolder.<clinit>() line: 17
      com.example.ledger.SettlementService.amountOwed(String) line: 43
      jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Object, Object[]) line: 104
      java.lang.reflect.Method.invoke(Object, Object[]) line: 565
      org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(Object, Method, Object[]) line: 359
      org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(Object, Method, Object[], MethodProxy) line: 715
      com.example.ledger.SettlementService$$SpringCGLIB$$0.amountOwed(String)
      com.example.ledger.PaymentController.lambda$settlement$0(String) line: 47
      java.util.concurrent.CompletableFuture$AsyncSupply.run() line: 1789
      java.util.concurrent.ThreadPerTaskExecutor$TaskRunner.run() line: 291
      java.lang.VirtualThread.run(Runnable) line: 475
      jdk.internal.vm.Continuation.enterSpecial(Continuation, boolean, boolean)
    ]
  }
  
  jdk.VirtualThreadPinned {
    startTime = 18:45:13.547 (2026-09-28)
    duration = 32.0 ms
    blockingOperation = "Contended monitor enter"
    pinnedReason = "Freeze or preempt failed (2)"
    carrierThread = "ForkJoinPool-1-worker-1" (javaThreadId = 57)
    eventThread = "tomcat-handler-236" (javaThreadId = 505, virtual)
    stackTrace = [
      java.lang.ClassLoader.loadClass(String, boolean) line: 549
      org.springframework.boot.loader.net.protocol.jar.JarUrlClassLoader.loadClass(String, boolean) line: 107
      org.springframework.boot.loader.launch.LaunchedClassLoader.loadClass(String, boolean) line: 91
      java.lang.ClassLoader.loadClass(String) line: 502
      org.springframework.web.servlet.mvc.method.annotation.AsyncTaskMethodReturnValueHandler.supportsReturnType(MethodParameter) line: 47
      org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite.selectHandler(Object, MethodParameter) line: 86
      org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite.handleReturnValue(Object, MethodParameter, ModelAndViewContainer, NativeWebRequest) line: 73
      org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletWebRequest, ModelAndViewContainer, Object[]) line: 135


```
- Duration: 223ms
- Pinning reason reported: VM call to com.example.ledger.FeeTableHolder.<clinit> on stack (blocking operation LockSupport.park)
- Frame that cannot unmount: FeeTableHolder.<clinit>, the static initializer. Once your deeper stack shows the frames between Thread.sleep and it, confirm this against what you see.
- Call that blocks inside it: Thread.sleep(Duration), reached from FeeTableClient.fetchTable()

### Why "replace synchronized with ReentrantLock" is old advice, and why it buys nothing here
(Two sentences, in your own words.)
1. Java 24 stopped monitors from pinning. 
2. This code has no synchronized block anyway; the blocking is inside a class initializer, so a ReentrantLock would change nothing.

## 6. Fix (step 9)
What changed: the fee table is fetched once when the FeeTable bean is created at startup, before the server accepts traffic
Why result semantics are identical: same client, same rates, same fallback, and PaymentControllerIT still gives 124469
Concurrency test: `FeeTableTest.tableIsLoadedOnceAndSafeUnderConcurrentFirstUse`

## 7. Final before / after (cold start)
| Run | Throughput (req/s) | p50 | p95 | p99 | Pinned events |
|---|---|---|---|---|---|
| Planted defect (`pinned.jfr`) | ___ | ___ | ___ | ___ | ___ |
| Fixed (`fixed.jfr`) | 968.3 | 157.41ms | 459.88ms | 750.9ms | 0 |

I ran a 3 request warm-up  before the final recording because a cold run still showed unrelated class-loading contention events

fixed-cold.jfr is included for reference showing those events disappear once warmed.

## 8. Pull request
- **Change:** replaced the FeeTableHolder static-initializer lookup with an eagerly-constructed FeeTable Spring bean, removing the pinning defect while keeping virtual threads enabled.

- **Risk:** startup now blocks briefly (~200ms) on the downstream fetch before the app can serve traffic; if the downstream call fails at startup the app won't start

- **Rollback:** revert this commit; the previous FeeTableHolder version differs only in when the fetch happens, not in the data returned.
