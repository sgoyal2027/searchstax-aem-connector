# Post-install smoke test against a running AEM author instance (6.5, AMS, or Cloud SDK local).
# Usage:
#   .\scripts\smoke-test-author.ps1
#   .\scripts\smoke-test-author.ps1 -AemPort 4504
#   .\scripts\smoke-test-author.ps1 -AemPort 4502 -InstallPackage

param(
    [string]$AemHost = "localhost",
    [int]$AemPort = 4502,
    [string]$User = "admin",
    [string]$Password = "admin",
    [switch]$UseHttps,
    [switch]$InstallPackage
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

$scheme = if ($UseHttps) { "https" } else { "http" }
$baseUrl = "${scheme}://${AemHost}:${AemPort}"
$pair = "${User}:${Password}"
$bytes = [System.Text.Encoding]::ASCII.GetBytes($pair)
$authHeader = "Basic " + [Convert]::ToBase64String($bytes)
$basicCred = New-Object System.Management.Automation.PSCredential($User, (ConvertTo-SecureString $Password -AsPlainText -Force))

function Invoke-AemGet {
    param([string]$Path)
    $uri = "$baseUrl$Path"
    try {
        $response = Invoke-WebRequest -Uri $uri -Headers @{ Authorization = $authHeader } -TimeoutSec 30 -UseBasicParsing
        return @{ Ok = $true; Status = [int]$response.StatusCode; Body = $response.Content }
    } catch {
        $status = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        return @{ Ok = $false; Status = $status; Error = $_.Exception.Message }
    }
}

function Assert-Ok {
    param(
        [string]$Name,
        [hashtable]$Result,
        [int[]]$AllowedStatus = @(200)
    )
    if ($Result.Ok -and ($AllowedStatus -contains $Result.Status)) {
        Write-Host "[PASS] $Name ($($Result.Status))" -ForegroundColor Green
        return
    }
    if (-not $Result.Ok -and $Result.Status -and ($AllowedStatus -contains $Result.Status)) {
        Write-Host "[PASS] $Name ($($Result.Status))" -ForegroundColor Green
        return
    }
    $detail = if ($Result.Error) { $Result.Error } else { "HTTP $($Result.Status)" }
    Write-Host "[FAIL] $Name - $detail" -ForegroundColor Red
    throw "Smoke test failed: $Name"
}

function Install-ConnectorPackage {
    $zip = Get-ChildItem "$Root\all\target\searchstax-aem-connector.all-*.zip" | Select-Object -First 1
    if (-not $zip) {
        throw "Package not found. Run mvn clean install first."
    }
    Write-Host "Installing $($zip.Name) via Package Manager on $baseUrl ..."
    $installUrl = "$baseUrl/crx/packmgr/service.jsp?cmd=install"
    $boundary = [System.Guid]::NewGuid().ToString()
    $fileBytes = [System.IO.File]::ReadAllBytes($zip.FullName)
    $fileEnc = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes)
    $bodyLines = @(
        "--$boundary",
        "Content-Disposition: form-data; name=`"package`"; filename=`"$($zip.Name)`"",
        "Content-Type: application/octet-stream",
        "",
        $fileEnc,
        "--$boundary",
        "Content-Disposition: form-data; name=`"force`"",
        "",
        "true",
        "--$boundary--",
        ""
    )
    $body = $bodyLines -join "`r`n"
    Invoke-WebRequest -Uri $installUrl -Method Post `
        -Headers @{ Authorization = $authHeader; "Content-Type" = "multipart/form-data; boundary=$boundary" } `
        -Body $body -TimeoutSec 120 -UseBasicParsing | Out-Null
    Write-Host "Waiting for package install to complete..."
    Start-Sleep -Seconds 25
}

Write-Host "SearchStax Connector - author smoke test" -ForegroundColor Cyan
Write-Host "Target: $baseUrl"
Write-Host ""

if ($InstallPackage) {
    Install-ConnectorPackage
}

Write-Host "Checking AEM reachability..."
Assert-Ok -Name "AEM system health" -Result (Invoke-AemGet -Path "/libs/granite/core/content/login.html") -AllowedStatus @(200, 302)

Write-Host "Checking OSGi bundle..."
$bundles = Invoke-AemGet -Path "/system/console/bundles.json"
Assert-Ok -Name "OSGi console" -Result $bundles
$bundleRow = ($bundles.Body | ConvertFrom-Json).data | Where-Object { $_.symbolicName -eq "searchstax-aem-connector.core" }
if (-not $bundleRow) {
    throw "Smoke test failed: searchstax-aem-connector.core bundle not found. Install the all package first."
}
if ($bundleRow.state -ne "Active") {
    throw "Smoke test failed: searchstax-aem-connector.core is $($bundleRow.state) (expected Active). Check OSGi console for unresolved imports."
}
Write-Host "[PASS] Core bundle active ($($bundleRow.version))" -ForegroundColor Green

$wizardGets = @(
    "/bin/searchstaxconnector/wizard/initial-setup-load",
    "/bin/searchstaxconnector/wizard/api-config-load",
    "/bin/searchstaxconnector/wizard/metadata-field-mappings-load",
    "/bin/searchstaxconnector/wizard/language-mappings-load",
    "/bin/searchstaxconnector/wizard/full-index-load",
    "/bin/searchstaxconnector/wizard/site-application-mappings-load",
    "/bin/searchstaxconnector/wizard/indexing-report?limit=10"
)

Write-Host "Checking wizard servlets..."
foreach ($path in $wizardGets) {
    Assert-Ok -Name $path -Result (Invoke-AemGet -Path $path)
}

Write-Host "Checking Tools navigation overlay..."
$tools = Invoke-AemGet -Path "/mnt/overlay/cq/core/content/nav/tools.1.json"
Assert-Ok -Name "Tools nav" -Result $tools -AllowedStatus @(200, 302)
if ($tools.Ok -and ($tools.Body -match "Searchstax|initial-setup-wizard")) {
    Write-Host "[PASS] SearchStax entry found in Tools nav" -ForegroundColor Green
} else {
    throw "Smoke test failed: SearchStax Tools nav entry not found in overlay response"
}

Write-Host ""
Write-Host "Author smoke test PASSED for $baseUrl" -ForegroundColor Green
