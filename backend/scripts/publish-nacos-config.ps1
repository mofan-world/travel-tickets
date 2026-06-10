[CmdletBinding()]
param(
    [string] $ServerAddr = $env:NACOS_SERVER_ADDR,
    [string] $Namespace = $env:NACOS_NAMESPACE,
    [string] $Group = $env:NACOS_GROUP,
    [string] $DataId = $env:NACOS_DATA_ID,
    [string] $ConfigFile = "",
    [int] $Retries = 30,
    [int] $DelaySeconds = 2
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ServerAddr)) {
    $ServerAddr = "127.0.0.1:8848"
}
if ([string]::IsNullOrWhiteSpace($Group)) {
    $Group = "DEFAULT_GROUP"
}
if ([string]::IsNullOrWhiteSpace($DataId)) {
    $DataId = "travel-ticket-service.yaml"
}
if ([string]::IsNullOrWhiteSpace($ConfigFile)) {
    $ConfigFile = Join-Path $PSScriptRoot "..\src\main\resources\nacos\travel-ticket-service.yaml"
}

if (-not (Test-Path -LiteralPath $ConfigFile)) {
    throw "Nacos config file was not found: $ConfigFile"
}

$baseUrl = $ServerAddr.TrimEnd("/")
if ($baseUrl -notmatch "^https?://") {
    $baseUrl = "http://$baseUrl"
}

$ready = $false
$healthUrl = "$baseUrl/nacos/v1/console/health/readiness"
for ($attempt = 1; $attempt -le $Retries; $attempt++) {
    try {
        Invoke-RestMethod -Method Get -Uri $healthUrl -TimeoutSec 3 | Out-Null
        $ready = $true
        break
    }
    catch {
        if ($attempt -eq $Retries) {
            break
        }
        Start-Sleep -Seconds $DelaySeconds
    }
}

if (-not $ready) {
    throw "Nacos is not ready at $baseUrl after $Retries attempts."
}

$content = Get-Content -Raw -Encoding UTF8 -LiteralPath $ConfigFile
$body = @{
    dataId = $DataId
    group = $Group
    type = "yaml"
    content = $content
}

if (-not [string]::IsNullOrWhiteSpace($Namespace)) {
    $body.tenant = $Namespace
}

$publishUrl = "$baseUrl/nacos/v1/cs/configs"
$result = Invoke-RestMethod `
    -Method Post `
    -Uri $publishUrl `
    -ContentType "application/x-www-form-urlencoded" `
    -Body $body `
    -TimeoutSec 10

if (("$result").ToLowerInvariant() -ne "true") {
    throw "Failed to publish Nacos config. Response: $result"
}

$namespaceLabel = if ([string]::IsNullOrWhiteSpace($Namespace)) { "public" } else { $Namespace }
Write-Host "Published Nacos config: dataId=$DataId, group=$Group, namespace=$namespaceLabel"
