# JMH evidence index

| File | Provenance | Status |
|---|---|---|
| `../results.json` | Original `C:\Users\Dell\IdeaProjects\benchmarks\results.json`, copied byte-for-byte during recovery | Historical three-method run on Java 25.0.4; not a new run of this build |
| `runs/<id>/results.json` | Created only when the student runs `run-benchmarks.ps1` | Pending |
| `runs/<id>/dce-results.json` | Same command, DCE demonstration | Pending; no original raw DCE output was found |
| `runs/<id>/suite.txt`, `dce.txt`, `manifest.json` | Console output and run identity from that command | Pending |

The historical results contain JMH 1.37, Java 25.0.4+7-LTS-189, five warmup
iterations, ten measurement iterations and three forks for each real method.
They contain allocation metrics, but no machine identity or DCE demonstration.
Do not invent that missing provenance or relabel them as a fresh measurement.

After reviewing a new run, retain its raw files and commit its manifest; update
the report and root results together. Never commit the generated target jars.
