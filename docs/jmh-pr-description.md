# Reproducible Java 25 JMH benchmarks and verified evidence

## Change

Include the missing benchmarks module in a Maven reactor with the Ledger
service and provide a Java-25-only one-command PowerShell build/run script.
Preserve a plain service dependency and attach its executable Boot jar with
the `exec` classifier; JMH remains outside the service dependency graph.
Keep the discarded-result benchmark and both observable-result fixes in source.

## Measured result

The student's 29 October 2026 run used Oracle Java 25.0.4+7-LTS-189 and JMH 1.37,
with five warmups, ten measurements and three forks per method, with `-prof gc`.
The final JSON and full console logs are committed under
`evidence/jmh-20261029-133647/`, with source commit and jar hash in the manifest.

The return and Blackhole variants recorded 0.649434241 +/- 0.0117840436 us/op
and 0.721244454 +/- 0.111167736 us/op respectively. Their 99.9% intervals overlap:
this comparison is inconclusive. The discarded-result score was
0.000866378407 +/- 0.00000410393989 us/op. Fixed/discarded point-estimate ratios
were 749.596522 (return) and 832.482029 (Blackhole); this demonstrates a broken
benchmark's misleading result, not a speedup in the running service.

The real-method time/allocation table and full precision sources are in
`docs/benchmarks.md`. No optimization win is claimed by comparing the three
different operations, and warmup sufficiency is not asserted for every method.

## Validation

The student's build-only check passed on Java 25. A separate independent clone
at 2ff5699 also passed the documented build-only command: one payment integration
test, no failures/errors/skips, and all six benchmark methods discovered.
That clone used the same machine and existing Maven cache, not CI or a second
machine. The full performance run was performed by the student, not the agent.

## Risk and monitoring

On this branch the executable app jar now ends `-exec.jar`; attempting to launch
the plain jar as an app would fail. Watch the reactor build/test result and
JMH discovery before measuring. Inspect failed manifests, missing JSON entries,
Java version, benchmark-jar hash and confidence intervals before accepting results.
Compiler Blackholes are experimental; the recorded mode matches across suites.
Laptop power was connected, while power mode and other unrecorded settings are
unknown. Generated assembly was not inspected.

## Rollback

To undo the build integration, revert commit `f331856` on the JMH feature branch
using an ordinary revert commit, resolving any later documentation conflicts
against the intended rollback. This restores the earlier service packaging and
removes the integrated benchmark build/runner. Retain raw evidence separately.
Do not reset, rebase, force-push or modify main. No payment behavior change or
runtime deployment is part of this benchmark PR.

## Evidence

- [README](https://github.com/Danny-Kyle/javaMod1lab4/blob/feature/jmh-benchmark/README.md): one-command reproduction and observed elapsed duration.
- [Report](https://github.com/Danny-Kyle/javaMod1lab4/blob/feature/jmh-benchmark/docs/benchmarks.md): scores/errors/units, allocations, inconclusive comparison,
  warmup assessment and three limits with proposed follow-up measurements.
- [Final results JSON](https://github.com/Danny-Kyle/javaMod1lab4/blob/feature/jmh-benchmark/results.json).
- [Raw evidence archive](https://github.com/Danny-Kyle/javaMod1lab4/tree/feature/jmh-benchmark/evidence/jmh-20261029-133647): raw results, logs, manifest, hashes and context.
- [Clean-clone build log](https://github.com/Danny-Kyle/javaMod1lab4/blob/feature/jmh-benchmark/evidence/clean-clone-build-2026-10-29.txt): independent-clone build/discovery log.
