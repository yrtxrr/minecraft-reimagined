# Requires Windows PowerShell 5.1+ or PowerShell7 and Python3.11+ (stdlib only).
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [ValidateSet('betterend','nullscape')][string]$End = 'nullscape',
    [string]$CoreJar = '',
    [string]$Bindings = '',
    [switch]$Install,
    [switch]$InstallAvailable,
    [switch]$UseExistingPlan,
    [switch]$PinnedOnly,
    [switch]$IncludeOptional,
    [switch]$OfficialFtbMaven,
    [string]$LocalFiles = '',
    [switch]$AllowBeta
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$scriptPath = Join-Path $PSScriptRoot 'install.py'
$pyCommand = Get-Command py -ErrorAction SilentlyContinue
$pyArguments = @()
if ($pyCommand) { $python = $pyCommand.Source; $pyArguments = @('-3') }
else {
    $pyCommand = Get-Command python -ErrorAction SilentlyContinue
    if (-not $pyCommand -or $pyCommand.Source -like '*WindowsApps*') {
        throw 'Install Python3.11+ from python.org, then rerun. No software will be installed automatically.'
    }
    $python = $pyCommand.Source
}
& $python @pyArguments -c 'import sys; sys.exit(0 if sys.version_info >= (3,11) else 1)'
if ($LASTEXITCODE -ne 0) { throw 'Python3.11+ required' }
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
if (-not $CoreJar) { $CoreJar = Join-Path $repoRoot 'payload/reimagined-0.1.0-alpha.jar' }
if (-not $UseExistingPlan) {
    $resolveArguments = @($scriptPath, 'resolve', '--output', $outputPath, '--end', $End)
    if ($Bindings) { $resolveArguments += @('--bindings', $Bindings) }
    if ($PinnedOnly) { $resolveArguments += '--pinned-only' }
    if ($IncludeOptional) { $resolveArguments += '--include-optional' }
    if ($OfficialFtbMaven) { $resolveArguments += '--ftb-maven' }
    if ($LocalFiles) { $resolveArguments += @('--local-files', $LocalFiles) }
    if ($AllowBeta) { $resolveArguments += '--allow-beta' }
    & $python @pyArguments @resolveArguments
    $resolveExit = $LASTEXITCODE
    if ($resolveExit -ne 0 -and $resolveExit -ne 2) { throw "Resolution failed ($resolveExit); inspect report" }
    if ($resolveExit -eq 2) { Write-Warning 'Some targets remain unresolved. All candidates are retained in INSTALL_REPORT.md.' }
}
if ($Install -or $InstallAvailable) {
    $installArguments = @($scriptPath, 'install', '--output', $outputPath, '--core-jar', $CoreJar)
    if ($LocalFiles) { $installArguments += @('--local-files', $LocalFiles) }
    if ($InstallAvailable) { $installArguments += '--allow-incomplete' }
    & $python @pyArguments @installArguments
    if ($LASTEXITCODE -ne 0) { throw 'Install stopped safely; profiles have not been certified. Inspect the report/error. No existing worlds were changed.' }
    Write-Host 'Import Reimagined-candidate.mrpack into Prism, or copy client files into a separate NeoForge1.21.1 instance.'
    Write-Host 'Server folder needs official NeoForge21.1.264 server install and your own EULA consent. See Windows instructions.'
} else {
    Write-Host 'Resolve-only complete. Review INSTALL_REPORT.md and rerun with -UseExistingPlan -Install (or explicitly -InstallAvailable).'
}
