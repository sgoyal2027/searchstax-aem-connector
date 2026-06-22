# Cross-platform build verification for AEM Cloud, AMS, and 6.5.

# Usage: .\scripts\cross-platform-verify.ps1

#

# Requires client AEM versions in .mvn/maven.config (copy from .mvn/maven.config.example)

# or build.properties (copy from build.properties.example).



$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

Set-Location $Root



function Write-Step($msg) {

    Write-Host ""

    Write-Host "==> $msg" -ForegroundColor Cyan

}



function Assert-ClientVersionsConfigured {

    $mvnConfig = Join-Path $Root ".mvn\maven.config"

    if (Test-Path $mvnConfig) {

        $hasVersion = $false

        Get-Content $mvnConfig | ForEach-Object {

            $line = $_.Trim()

            if ($line -and -not $line.StartsWith("#") -and $line -match "^-D(aem\.sdk\.api|uber\.jar\.version)=.+") {

                $hasVersion = $true

            }

        }

        if ($hasVersion) { return @() }

    }



    $propsFile = Join-Path $Root "build.properties"

    if (Test-Path $propsFile) {

        $args = @()

        Get-Content $propsFile | ForEach-Object {

            $line = $_.Trim()

            if ($line -and -not $line.StartsWith("#") -and $line -match "^([^=]+)=(.*)$") {

                $name = $matches[1].Trim()

                $value = $matches[2].Trim()

                if ($name -and $value) {

                    $args += "-D$name=$value"

                }

            }

        }

        if ($args.Count -gt 0) { return $args }

    }



    throw @"

Client AEM versions not configured.



Copy .mvn/maven.config.example to .mvn/maven.config (recommended) or build.properties.example to build.properties, then set:

  aem.sdk.api      - your Cloud SDK version

  uber.jar.version - your AEM 6.5 uber-jar version

"@

}



function Invoke-Maven([string[]]$MavenArgs) {

    Write-Host ("mvn " + ($MavenArgs -join " "))

    $previousErrorAction = $ErrorActionPreference

    $ErrorActionPreference = "Continue"

    try {

        & mvn @MavenArgs

        if ($LASTEXITCODE -ne 0) {

            throw "Maven failed: mvn $($MavenArgs -join ' ')"

        }

    } finally {

        $ErrorActionPreference = $previousErrorAction

    }

}



$ExtraArgs = Assert-ClientVersionsConfigured



Write-Step "1/4 AEM as a Cloud Service - full build + AEM Analyser"

Invoke-Maven @($ExtraArgs + @("clean", "install", "-q"))



Write-Step "2/4 Package smoke tests (JUnit)"

Invoke-Maven @($ExtraArgs + @("test", "-pl", "core", "-Dtest=ConnectorCrossPlatformSmokeTest", "-q"))



Write-Step "3/4 AEM 6.5 / AMS - core compile + test against uber-jar (classic profile)"

Invoke-Maven @($ExtraArgs + @("compile", "test", "-pl", "core", "-Pclassic", "-P!cloudservice", "-q"))



Write-Step "4/5 Cloud CI/CD artifact safety (embedded ui.apps filter, no structure package)"

& (Join-Path $Root "scripts\verify-cloud-cicd-artifacts.ps1")



Write-Step "5/5 Artifact summary"

$AllZip = Get-ChildItem "all\target\searchstax-aem-connector.all-*.zip" | Select-Object -First 1

$CoreJar = Get-ChildItem "core\target\searchstax-aem-connector.core-*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1



Write-Host "Container package : $($AllZip.FullName) ($([math]::Round($AllZip.Length/1MB, 2)) MB)"

Write-Host "Core bundle       : $($CoreJar.FullName) ($([math]::Round($CoreJar.Length/1KB, 0)) KB)"

Write-Host ""

Write-Host "Cross-platform verify PASSED (Cloud analyser + 6.5 classic compile + package smoke)." -ForegroundColor Green

Write-Host "For live instance checks run: .\scripts\smoke-test-author.ps1 -AemHost localhost -AemPort 4502"

