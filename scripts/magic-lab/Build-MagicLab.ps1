[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$WorkDirectory,
    [Parameter(Mandatory=$true)][string]$CoreJar
)
$ErrorActionPreference = 'Stop'
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Install Git for Windows from git-scm.com.' }
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    throw 'Set JAVA_HOME to a full JDK21 (not a JRE); restart PowerShell after installation.'
}
$javaVersion = & (Join-Path $env:JAVA_HOME 'bin\java.exe') -version 2>&1 | Out-String
if ($javaVersion -notmatch 'version "21[.\"]') { throw 'JAVA_HOME must select Java21.' }
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
& py -3 -c 'import sys; sys.exit(0 if sys.version_info >= (3,11) else 1)'
if ($LASTEXITCODE -ne 0) { throw 'Python3.11+ with py launcher is required.' }
& py -3 (Join-Path $PSScriptRoot 'magic_lab.py') --work $WorkDirectory --core $CoreJar
if ($LASTEXITCODE -ne 0) { throw 'Magic Lab stopped. Inspect the timestamped logs in WorkDirectory. R4 was not modified.' }
Write-Host 'Import the profile ZIPs from WorkDirectory\profiles into Prism. No game was launched and no EULA was accepted.'
