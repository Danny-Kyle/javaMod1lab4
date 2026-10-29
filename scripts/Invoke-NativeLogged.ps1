function Invoke-NativeLogged {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][string]$LogPath
    )

    $previousPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 wraps native stderr in ErrorRecord objects.
        # Keep their text in the log, but judge Java failure by its exit code.
        $ErrorActionPreference = 'Continue'
        & $Executable @ArgumentList 2>&1 |
            ForEach-Object { $_.ToString() } -ErrorAction Stop |
            Tee-Object -FilePath $LogPath -ErrorAction Stop
        $nativeExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }

    if ($nativeExitCode -ne 0) {
        throw "Native command exited with code $nativeExitCode. Inspect $LogPath"
    }
}
