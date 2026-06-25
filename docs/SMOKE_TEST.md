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
| 7 | SearchStax Search component | Renders on publish |
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
| Wizard save 404 on Cloud | Servlet not in `SlingServletResolver.cfg.json` — re-run `ConnectorCrossPlatformSmokeTest` |
| Queue empty after publish | Connector disabled in Initial Setup or path excluded |
| Bundle not active | Check `/system/console/bundles` for unresolved imports; verify AEM version ≥ 6.5 |
| Analyser failure | Review `all/target/cp-conversion` logs; remove non-Cloud APIs from bundle |

## CI recommendation

Add to pipeline:

```yaml
- mvn clean install
- mvn test -pl core -Pclassic
```

Run `smoke-test-author.ps1` against a dedicated QA author after deploy.
