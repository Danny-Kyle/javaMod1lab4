# JMH criterion status after the completed run

This is a JMH progress check, not a marker-awarded score or the final three-lab matrix. Targets require the marker to accept both Meets and Exceeds conditions.

| Ref | Rubric criterion | Target band | Status | Evidence / limitation |
|---|---|---|---|---|
| A1 | Benchmarks module | 4 | Verified | benchmarks/pom.xml; benchmark-build/pom.xml; service plain jar and isolated JMH dependencies; successful clean-clone log. |
| A2 | Three real methods chosen | 4 | Verified | docs/benchmarks.md methods table; all three wrappers call the real extracted methods. |
| A3 | Deliberately broken benchmark | 4 | Verified with stated limit | DCE source, raw discarded score and explanation of unobservable loop work; generated assembly was not inspected. |
| A4 | Fixed with return or Blackhole | 4 | Verified | Both fixes and point-estimate ratios retained; overlapping fixed intervals explicitly inconclusive. |
| A5 | Inputs in @State | 4 | Verified | Each class uses Scope.Benchmark and Setup; payment shape, field messages and seeded calculation input are documented. |
| A6 | Run configuration | 3 | Meets; Exceeds not established | All required counts/mode/units are present in source, JSON and logs. Warmup was inspected but convergence is not proved for every method. |
| A7 | Suite executed with JSON output | 4 | Companion manifest caveat | Final results.json and exact command are retained. JDK is in raw JSON; machine identity is in the companion manifest, not added to the raw JSON. Strict acceptance of that Exceeds wording remains the marker's decision. |
| A8 | Allocation profile | 4 | Verified | Every method has profiler bytes/op; response-record and joining source sites are named without claiming unavailable allocation-stack attribution. |
| A9 | Results with error bars | 4 | Verified | Scores/errors/units and 99.9% confidence are sourced; the comparable fixed pair is inconclusive. |
| A10 | Limits section | 4 | Prepared for student review | Three student-supplied limits retained/reviewed; at the student's request a proposed further measurement was added for each, explicitly not claimed as performed. |
| B1 | Deliverable 1 — benchmarks module | 4 | Verified build; completed run evidence | One-command runner, clean-clone build, full user-run JSON/logs and observed duration. Clean-clone check did not repeat performance measurements. |
| B2 | Deliverable 2 — results and the broken-vs-fixed numbers | 4 | Verified | Root results.json, archived DCE JSON, both fixes and broken benchmark source remain available. |
| B3 | Deliverable 3 — docs/benchmarks.md | 4 | Prepared for final reading | Correct table, explicit inconclusive comparison, three limits and cautious developer conclusion. |
| C1 | Build and run integrity | 3 | Meets; Exceeds pending | Same-machine independent clone builds/tests/discovers all methods. No second-machine or CI run has been claimed. |
| C2 | Accuracy of stated figures | 4 | Verified | Every table value was checked against its raw JSON; original bytes are archived and hashed. |
| C3 | Evidence quality | 4 | Verified | Named report/root JSON, units, direct evidence index, raw logs, context and clean-clone proof; no template blanks. |
| C4 | Statistical honesty | 3 | Meets; Exceeds not established | Prior overstatement is withdrawn and overlap respected. No unsupported assertion that an old result was proved to be noise; no controlled historical comparison exists. |

See [report](benchmarks.md), [evidence index](../evidence/README.md), and [proposed PR description](jmh-pr-description.md). Changes remain local; main and the existing remote branch/PR have not been changed.
