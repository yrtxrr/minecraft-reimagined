[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$WorkDirectory,
    [Parameter(Mandatory=$true)][string]$MinecraftDirectory,
    [Parameter(Mandatory=$true)][string]$OutputZip
)
$ErrorActionPreference = 'Stop'
& py -3 (Join-Path $PSScriptRoot 'collect-diagnostics.py') --instance $MinecraftDirectory --output $OutputZip
if ($LASTEXITCODE -ne 0) { throw 'Game log collection failed.' }
Write-Host 'Game logs collected. Also attach the timestamped build logs and artifacts.json from WorkDirectory after reviewing their contents.'
