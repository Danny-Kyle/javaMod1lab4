# Ledger settlement — JMH lab

From this repository's root, on a quiet machine with JDK 25, Maven and Docker running:

```powershell
powershell -NoProfile -File .\run-benchmarks.ps1
```

This one command verifies Java 25, builds and tests the service and benchmarks in
one Maven reactor, checks benchmark discovery, then runs the three real methods
and the dead-code-elimination demonstration with the GC profiler. It refuses a
dirty source checkout. Commit changes first. It does not switch branches, stop
an application, replace existing evidence, or change the database used by an
already-running application. Integration tests use their own Postgres container.

If Testcontainers needs the existing Docker Desktop TCP endpoint described in
your environment setup, set `$env:DOCKER_HOST='tcp://localhost:2375'` first. This
script does not enable that endpoint or change Docker settings.

`benchmark-build/pom.xml` aggregates the service in the repository root and the
`benchmarks` module. The service's main jar stays a plain compile dependency;
the executable Spring Boot jar is `target/ledger-settlement-0.0.1-SNAPSHOT-exec.jar`.
JMH dependencies belong only to `benchmarks`. No manual jar install or sibling
folder is required. `benchmarks/target/benchmarks.jar` is the executable JMH jar.

Use `powershell -NoProfile -File .\run-benchmarks.ps1 -BuildOnly` to build, run
tests and list benchmarks without starting measurements. For a different Java
25 installation, pass `-JdkHome 'C:\path\to\jdk-25'`; other major versions fail.

The exact suite command, run by the script with an absolute output path, is:

```powershell
java -jar benchmarks\target\benchmarks.jar MappingBenchmark StringJoinBenchmark SettlementCalcBenchmark -prof gc -rf json -rff results.json
```

The DCE command uses `DeadCodeEliminationDemoBenchmark` with the same profiler
and JSON options. No warmup, measurement or fork counts are reduced. The source
annotations require five warmup iterations, ten measurement iterations and
three forks. This is a long run: the saved historical JSON reports ten seconds
per iteration. No verified complete-run wall time is available yet; the new
runner records the actual elapsed benchmark time in its manifest. Do not use
the starter runbook's one-second-iteration estimate.

Each run gets a new `evidence/runs/<timestamp>-<id>/` directory containing
`results.json`, `dce-results.json`, both console logs and a manifest with source
commit, Java/Maven versions, machine details and jar hash. These new run folders
are ignored until reviewed. Send those files back for verification before
promoting a final run to the committed root `results.json` and evidence index.

The root `results.json` currently preserves the original saved Java 25 run;
it is not a claim that the integrated build has been remeasured. See
[the evidence index](evidence/README.md) and [the report](docs/benchmarks.md).
