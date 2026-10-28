[CmdletBinding()]
param(
    [string]$JdkHome = 'C:\Program Files\Java\jdk-25.0.4',
    [switch]$BuildOnly
)
$ErrorActionPreference = 'Stop'
$java = Join-Path $JdkHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw "JDK missing: $java" }
$javaVersion = (& $java --version | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '(?m)^(?:openjdk|java) 25(?:\.|\s|-)') {
    throw "Java 25 is required. Found: $javaVersion"
}
$env:JAVA_HOME = $JdkHome
$env:Path = "$JdkHome\bin;$env:Path"
$maven = (Get-Command mvn.cmd -ErrorAction Stop).Source

Push-Location $PSScriptRoot
try {
    $status = & git status --short
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read git status.' }
    if ($status) { throw "Commit source changes before building. Current status:`n$($status -join "`n")" }
    $commit = (& git rev-parse HEAD | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot identify the source commit.' }
    # clean only affects this checkout's generated target directories.
    & $maven -B -f benchmark-build/pom.xml clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed; no benchmark was run.' }
    $jar = Join-Path $PSScriptRoot 'benchmarks\target\benchmarks.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'The benchmark jar was not built.' }
    & $java -jar $jar -l
    if ($LASTEXITCODE -ne 0) { throw 'JMH discovery failed.' }
    if ($BuildOnly) { return }

    $runName = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
    $out = Join-Path $PSScriptRoot "evidence\runs\$runName"
    New-Item -ItemType Directory -Path $out -ErrorAction Stop | Out-Null
    $suiteJson = Join-Path $out 'results.json'
    $dceJson = Join-Path $out 'dce-results.json'
    $manifest = [ordered]@{
        sourceCommit = $commit
        javaVersion = $javaVersion.Trim()
        mavenVersion = (& $maven -v | Out-String).Trim()
        machine = [Environment]::MachineName
        logicalProcessors = [Environment]::ProcessorCount
        operatingSystem = [Environment]::OSVersion.VersionString
        benchmarkJarSha256 = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
        startedAt = (Get-Date).ToString('o')
        suiteArguments = @('-jar', $jar, 'MappingBenchmark', 'StringJoinBenchmark', 'SettlementCalcBenchmark', '-prof', 'gc', '-rf', 'json', '-rff', $suiteJson)
        dceArguments = @('-jar', $jar, 'DeadCodeEliminationDemoBenchmark', '-prof', 'gc', '-rf', 'json', '-rff', $dceJson)
    }
    $manifestPath = Join-Path $out 'manifest.json'
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    $clock = [Diagnostics.Stopwatch]::StartNew()
    & $java @($manifest.suiteArguments) 2>&1 | Tee-Object -FilePath (Join-Path $out 'suite.txt')
    if ($LASTEXITCODE -ne 0) { throw "Suite failed; inspect $out" }
    & $java @($manifest.dceArguments) 2>&1 | Tee-Object -FilePath (Join-Path $out 'dce.txt')
    if ($LASTEXITCODE -ne 0) { throw "DCE demonstration failed; inspect $out" }
    $clock.Stop()
    $manifest.completedAt = (Get-Date).ToString('o')
    $manifest.benchmarkSeconds = $clock.Elapsed.TotalSeconds
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    Write-Host "Evidence saved without replacing older results: $out"
} finally {
    Pop-Location
}
