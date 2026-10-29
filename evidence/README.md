# JMH evidence index

The student completed run `20261029-133647-d8da31d5` on 29 October 2026.
Source identity and completion are in the raw manifest. Files were copied
byte-for-byte out of the ignored runs directory into the committed archive.

| File | Provenance and use |
|---|---|
| `../results.json` | Final three-real-method JSON; identical to archived results.json |
| `jmh-20261029-133647/results.json` | Unmodified raw Java 25 real-method suite output |
| `jmh-20261029-133647/dce-results.json` | Unmodified raw discarded/return/Blackhole results |
| `jmh-20261029-133647/suite.txt` | Full suite console output, including warmups and confidence level |
| `jmh-20261029-133647/dce.txt` | Full DCE console output and matching compiler-Blackhole mode |
| `jmh-20261029-133647/manifest.json` | Raw source commit, command arguments, exact Java/Maven build, machine, jar hash and elapsed time |
| `jmh-20261029-133647/verification.json` | Derived archive hashes, validation checks and point-estimate ratios; not raw benchmark output |
| `build-check-2026-10-29.txt` | Student-pasted successful build-only/test/discovery transcript before runner-only logging corrections |
| `historical/sibling-results.json` | Original recovered sibling JSON; retained for provenance, not the final run |

PowerShell 5.1 wrote the raw console logs as UTF-16. Some console symbols were
decoded incorrectly at capture time; the raw files are not edited to repair
them. Use the raw JSON for precise scores, errors and units. The report uses
ASCII +/- notation and values derived from JSON rather than the rounded console.

The earlier failed run `runs/20261029-132443-dcfbda09` remains local with an
empty JSON and its failure transcript; it is not measurement evidence. New
runner output goes into unique ignored run folders and is promoted only after
verification. Do not commit generated target jars or overwrite prior archives.
