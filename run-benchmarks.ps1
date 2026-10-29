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
. (Join-Path $PSScriptRoot 'scripts\Invoke-NativeLogged.ps1')

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
        status = 'Running'
        suiteArguments = @('-jar', $jar, 'MappingBenchmark', 'StringJoinBenchmark', 'SettlementCalcBenchmark', '-prof', 'gc', '-rf', 'json', '-rff', $suiteJson)
        dceArguments = @('-jar', $jar, 'DeadCodeEliminationDemoBenchmark', '-prof', 'gc', '-rf', 'json', '-rff', $dceJson)
    }
    $manifestPath = Join-Path $out 'manifest.json'
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    $clock = [Diagnostics.Stopwatch]::StartNew()
    try {
        Invoke-NativeLogged -Executable $java -ArgumentList $manifest.suiteArguments -LogPath (Join-Path $out 'suite.txt')
        $suiteResults = Get-Content -LiteralPath $suiteJson -Raw | ConvertFrom-Json
        if ($suiteResults -isnot [System.Array] -or $suiteResults.Count -ne 3) { throw 'Expected three completed real-method results.' }
        Invoke-NativeLogged -Executable $java -ArgumentList $manifest.dceArguments -LogPath (Join-Path $out 'dce.txt')
        $dceResults = Get-Content -LiteralPath $dceJson -Raw | ConvertFrom-Json
        if ($dceResults -isnot [System.Array] -or $dceResults.Count -ne 3) { throw 'Expected three completed DCE results.' }
    } catch {
        $manifest['status'] = 'Failed'
        $manifest['failedAt'] = (Get-Date).ToString('o')
        $manifest['failure'] = $_.Exception.Message
        $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding utf8
        throw
    }
    $clock.Stop()
    $manifest['completedAt'] = (Get-Date).ToString('o')
    $manifest['status'] = 'Completed'
    $manifest['benchmarkSeconds'] = $clock.Elapsed.TotalSeconds
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    Write-Host "Evidence saved without replacing older results: $out"
} finally {
    Pop-Location
}
