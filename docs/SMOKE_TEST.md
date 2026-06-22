# SearchStax AEM Connector — Smoke & Cross-Platform Test Guide

This connector ships as a **single artifact** for **AEM as a Cloud Service**, **AMS**, and **AEM 6.5**. Use the automated checks below before release; run live smoke tests on each target environment.

## Automated (local, no AEM instance)

From the repo root:

```powershell
.\scripts\cross-platform-verify.ps1
```

This runs:

| Step | Platform | What it validates |
|------|----------|-------------------|
| `mvn clean install` | **Cloud** | Full build, unit tests, **AEM Analyser** on container package |
| `mvn test -pl core -Pclassic` | **6.5 / AMS** | Core compiles and tests against AEM `uber-jar` APIs |
| `ConnectorCrossPlatformSmokeTest` | **All** | Container embeds core/ui.apps/ui.config; Cloud servlet allowlist; repoinit paths |

Equivalent manual commands:

```bash
mvn clean install
mvn clean test -pl core -Pclassic
mvn test -pl core -Dtest=ConnectorCrossPlatformSmokeTest
```

## Live author smoke test (6.5 / AMS / Cloud SDK)

### Prerequisites

1. Build: `mvn clean install`
2. AEM **author** running and reachable
3. Admin credentials

### Install + verify (6.5 / AMS)

1. **Package Manager** → upload `all/target/searchstax-aem-connector.all-*.zip` → Install
2. Run:

```powershell
.\scripts\smoke-test-author.ps1 -AemHost localhost -AemPort 4502 -User admin -Password admin
```

Or install via script:

```powershell
.\scripts\smoke-test-author.ps1 -InstallPackage
```

### Cloud Service

1. Embed `searchstax-aem-connector.all` in your Cloud Manager project's `pom.xml` (see [README](../README.md))
2. Deploy to **RDE** or **dev** environment via Cloud Manager
3. Run smoke script against the author URL:

```powershell
.\scripts\smoke-test-author.ps1 -AemHost author-pXXXX-eYYYY.adobeaemcloud.com -AemPort 443 -UseHttps -User admin -Password <token>
```

4. Confirm **Sling Servlet Resolver** allowlist is active (shipped in `ui.config`):

   `org.apache.sling.servlets.resolver.internal.SlingServletResolver.cfg.json`

5. If using **Dispatcher**, allow `/bin/searchstaxconnector/*` for admin wizard POST/GET from author.

### Cloud Sandbox (WKND / apps overlays)

Cloud Sandbox differs from a local AEM SDK in three ways that often look like “the connector deleted WKND”:

| Local SDK | Cloud Sandbox |
|-----------|----------------|
| You install the latest `all` zip manually | Pipeline redeploys embedded packages on **every** CM build |
| WKND may be installed once via Package Manager | WKND must live in your **Cloud Manager git repo** (`ui.apps` / `ui.content` modules) |
| `ui.apps.structure` is not installed | Must **never** embed or install `ui.apps.structure` on Cloud |

**After connector deploy, verify on Cloud author** (CRX DE or Developer Console):

| Path | Expected |
|------|----------|
| `/apps/wknd` | Present if WKND is part of your CM project |
| `/apps/cq/core/content/nav/tools` | Contains WKND **and** Searchstax entries |
| `/apps/searchstaxconnector` | Present |

**Safe Cloud Manager embed pattern** — embed only the container artifact:

```xml
<embedded>
    <groupId>com.searchstax.aem.connector</groupId>
    <artifactId>searchstax-aem-connector.all</artifactId>
    <type>zip</type>
    <target>/apps/<your-project>-packages/application/install</target>
</embedded>
```

Do **not** embed `searchstax-aem-connector.ui.apps`, `ui.config`, or `ui.apps.structure` separately.

**If WKND is missing on Sandbox after a pipeline run:**

1. Confirm your CM `pom.xml` uses a connector build that includes the merge-only `ui.apps` filter (commit `e7efeb1` or later).
2. Bump the connector dependency version, run a full CM pipeline (not only local `mvn install`).
3. Ensure WKND modules are in the CM git repository and not excluded from the pipeline.
4. Re-run the pipeline to restore WKND from git — the connector does not ship WKND content.

Build-time guard: `ConnectorCrossPlatformSmokeTest.embeddedUiAppsInContainer_usesSafeNavMergeFilter` validates the `all` zip that Cloud will deploy.

### CI/CD pipeline (full codebase deploy)

If your pipeline runs `mvn clean install` on the **entire repository**, Cloud Sandbox still only receives what the pipeline **uploads**. Building all modules creates several zips under `*/target/` — uploading the wrong ones deletes WKND.

| Artifact | Deploy to Cloud? |
|----------|------------------|
| `all/target/searchstax-aem-connector.all-*.zip` | **Yes — only this** |
| `ui.apps/target/*.zip` | **No** (already embedded in `all`) |
| `ui.config/target/*.zip` | **No** (already embedded in `all`) |
| `ui.apps.structure/target/*.zip` | **Never** (validation only; can delete `/apps` if installed) |

Add this step after `mvn clean install` in your CI/CD job:

```powershell
.\scripts\verify-cloud-cicd-artifacts.ps1
```

**Adobe Cloud Manager:** only the `all` module should have `cloudManagerTarget=all`. Other modules use `cloudManagerTarget=none` so CM does not deploy them standalone.

**If WKND is in the same git repo** (monorepo with WKND + connector): ensure WKND `ui.apps` / `ui.content` modules are embedded in your project `all` package. This connector repo does **not** ship WKND — Sandbox will not have `/apps/wknd` unless your CM project includes it.

**If WKND is in a separate repo:** connector pipeline cannot restore WKND; reinstall WKND via its own pipeline or Adobe sample content package.

### AMS notes

- Same install path as 6.5 (Package Manager on author)
- Repoinit provisions service user and `/conf/searchstaxconnector` — no separate permissions package required
- Incremental indexing runs on **author**; ensure publish replication agents are configured

## Manual checklist (all platforms)

| # | Check | Expected |
|---|--------|----------|
| 1 | Tools → SearchStax visible | All wizards open |
| 2 | Initial Setup → Save / reload | Values persist under `/conf/searchstaxconnector/settings/initialsetupconfig` |
| 3 | API Configuration → Save | Endpoints stored; connection test responds |
| 4 | Activate a page under configured root path | Sling job `searchstaxconnector/incremental-index` created |
| 5 | Indexing Report | QUEUED event appears with correlation ID |
| 6 | Full Index → Run (author only) | Status servlet returns progress JSON |
| 7 | SearchStax Search component | Renders on publish; respects maintenance mode |
| 8 | OSGi console | `searchstax-aem-connector.core` **Active** |

## Platform matrix

| Capability | AEM 6.5 | AMS | AEM Cloud |
|------------|---------|-----|-----------|
| Container package install | Package Manager | Package Manager | Maven embed + CM pipeline |
| Repoinit / service user | Yes (`ui.config`) | Yes | Yes |
| `/bin/searchstaxconnector/*` wizards | Open by default | Open by default | Requires allowlist (included) |
| AEM Analyser | N/A (informative if run locally) | N/A | **Required** — part of `mvn install` |
| Classic API compile (`-Pclassic`) | Validates 6.5 API compatibility | Same | Same artifact |

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| WKND missing on Cloud Sandbox only | CM pipeline deploys old connector `ui.apps` (replace nav filter) or WKND not in CM git; see [Cloud Sandbox](#cloud-sandbox-wknd--apps-overlays) |
| Wizard save 404 on Cloud | Servlet not in `SlingServletResolver.cfg.json` — re-run `ConnectorCrossPlatformSmokeTest` |
| Queue empty after publish | Connector disabled in Initial Setup, path excluded, or maintenance mode active |
| Bundle not active | Check `/system/console/bundles` for unresolved imports; verify AEM version ≥ 6.5 |
| Analyser failure | Review `all/target/cp-conversion` logs; remove non-Cloud APIs from bundle |

## CI recommendation

Add to pipeline:

```yaml
- mvn clean install
- mvn test -pl core -Pclassic
```

Run `smoke-test-author.ps1` against a dedicated QA author after deploy.
