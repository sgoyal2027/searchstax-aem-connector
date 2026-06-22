# Validates Maven artifacts before AEM Cloud / CI-CD deployment.
# Fails the build if packages that can delete WKND or other /apps overlays would be deployed.
#
# Usage (from repo root):
#   .\scripts\verify-cloud-cicd-artifacts.ps1
#
# Run after: mvn clean install

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

function Write-Ok($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red; exit 1 }

$AllZip = Get-ChildItem "all\target\searchstax-aem-connector.all-*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $AllZip) {
    Write-Fail "Missing container package. Run: mvn clean install"
}
Write-Ok "Container package found: $($AllZip.Name)"

# Only the container package may be deployed to Cloud Sandbox.
$DeployablePattern = "searchstax-aem-connector.all-.*\.zip$"
$ForbiddenDeployPatterns = @(
    "ui\.apps\.structure-.*\.zip$",
    "searchstax-aem-connector\.ui\.apps\.structure-.*\.zip$"
)

$ContentZips = Get-ChildItem -Recurse -Path @("all\target", "ui.apps\target", "ui.config\target", "ui.apps.structure\target") -Filter "*.zip" -ErrorAction SilentlyContinue
foreach ($zip in $ContentZips) {
    foreach ($forbidden in $ForbiddenDeployPatterns) {
        if ($zip.Name -match $forbidden) {
            Write-Host "[WARN] Built structure package exists: $($zip.FullName)" -ForegroundColor Yellow
            Write-Host "       Do NOT upload this zip in CI/CD. It is build-time validation only." -ForegroundColor Yellow
        }
    }
}

# Validate embedded ui.apps filter inside the container (what Cloud actually installs).
Add-Type -AssemblyName System.IO.Compression.FileSystem
$allArchive = [IO.Compression.ZipFile]::OpenRead($AllZip.FullName)
try {
    $uiAppsEntry = $allArchive.Entries | Where-Object {
        $_.FullName -like "*searchstax-aem-connector.ui.apps*" -and $_.FullName -like "*.zip"
    } | Select-Object -First 1
    if (-not $uiAppsEntry) {
        Write-Fail "Container does not embed ui.apps zip"
    }

    $tempUiApps = [IO.Path]::GetTempFileName() + ".zip"
    try {
        [IO.Compression.ZipFileExtensions]::ExtractToFile($uiAppsEntry, $tempUiApps, $true)
        $uiAppsArchive = [IO.Compression.ZipFile]::OpenRead($tempUiApps)
        try {
            $filterEntry = $uiAppsArchive.GetEntry("META-INF/vault/filter.xml")
            if (-not $filterEntry) {
                Write-Fail "Embedded ui.apps is missing META-INF/vault/filter.xml"
            }
            $reader = New-Object IO.StreamReader($filterEntry.Open())
            $filterXml = $reader.ReadToEnd()
            $reader.Close()

            if ($filterXml -notmatch '/apps/cq/core/content/nav/tools/Searchstax') {
                Write-Fail "Embedded ui.apps filter must target /apps/cq/core/content/nav/tools/Searchstax only"
            }
            if ($filterXml -notmatch 'mode="merge"') {
                Write-Fail 'Embedded ui.apps filter must use mode="merge"'
            }
            if ($filterXml -match 'root="/apps/cq/core/content/nav"') {
                Write-Fail "Embedded ui.apps uses unsafe replace filter on /apps/cq/core/content/nav (removes WKND Tools entries)"
            }
            Write-Ok "Embedded ui.apps filter is Cloud-safe (merge Searchstax nav only)"
        } finally {
            $uiAppsArchive.Dispose()
        }
    } finally {
        Remove-Item $tempUiApps -Force -ErrorAction SilentlyContinue
    }

    $structureEmbedded = $allArchive.Entries | Where-Object {
        $_.FullName -like "*ui.apps.structure*" -and $_.FullName -like "*.zip"
    }
    if ($structureEmbedded) {
        Write-Fail "Container embeds ui.apps.structure - remove it from all/pom.xml embeddeds"
    }
    Write-Ok "Container does not embed ui.apps.structure"
} finally {
    $allArchive.Dispose()
}

Write-Host ""
Write-Host "CI/CD deploy rule: upload ONLY this file to AEM Cloud / Package Manager:" -ForegroundColor Cyan
Write-Host "  $($AllZip.FullName)"
Write-Host ""
Write-Host "Do NOT deploy ui.apps, ui.config, or ui.apps.structure zips separately." -ForegroundColor Cyan
Write-Host "Cloud CI/CD artifact verification PASSED." -ForegroundColor Green
