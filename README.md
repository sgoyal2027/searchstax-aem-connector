# SearchStax AEM Connector

Production AEM connector for push-based indexing to SearchStax Site Search. Supports **AEM as a Cloud Service**, **AMS**, and **AEM 6.5** from a single Maven artifact.

## Modules

| Module | Purpose |
|--------|---------|
| `all` | Container content-package — **install this on AEM** |
| `core` | OSGi bundle (indexing, jobs, servlets, Sling models) |
| `ui.apps` | Admin wizards, site components, clientlibs, Tools navigation |
| `ui.config` | Repoinit, service user, OSGi configuration |
| `ui.apps.structure` | Vault repository structure validation |

## Build

```bash
mvn clean install
```

Output: `all/target/searchstax-aem-connector.all-1.0.0-SNAPSHOT.zip`

## Install on AEM 6.5 / AMS

1. Open **Package Manager** on the author instance.
2. Upload and install `searchstax-aem-connector.all-*.zip`.
3. Open **Tools → SearchStax** and complete configuration (see below).

## Install on AEM as a Cloud Service

Embed the connector `all` package in your Cloud Manager project (same pattern as [IntelligenceBank AEM Connector](https://help.intelligencebank.com/support/solutions/articles/51000505080)):

```xml
<dependency>
    <groupId>com.searchstax.aem.connector</groupId>
    <artifactId>searchstax-aem-connector.all</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <type>zip</type>
</dependency>
```

Configure `filevault-package-maven-plugin` embeddeds to install under your project package install path, then deploy via Cloud Manager.

**Cloud servlet access:** Ensure `/bin/searchstaxconnector/*` endpoints used by admin wizards are allowed in your dispatcher / Cloud Service servlet allowlists (`SlingServletResolver` cfg is shipped in `ui.config`).

## Operator guide

### Configuration wizards (Tools → SearchStax)

| Wizard | Purpose |
|--------|---------|
| **Initial Setup** | Enable connector, root/exclude paths, asset file types, maintenance mode |
| **API Configuration** | Update, select, discovery, and analytics endpoints + tokens |
| **Metadata Field Mapping** | AEM → Solr field mappings; mark fields as mandatory |
| **Site Application Mapping** | Map AEM site roots to SearchStax application IDs and search profiles |
| **Full Index** | Run or monitor a full reindex job |
| **Indexing Report** | View incremental queue audit events; double-click a row to reprocess |

Settings persist under `/conf/searchstaxconnector/settings/*`. Runtime queues and audit data live under `/var/searchstaxconnector/incremental-index/*`.

### Indexing modes

**Incremental (default on publish):**

```
Replication → Sling Job → JCR pending queue → scheduler → SearchStax update API
```

- Publish/unpublish/delete events enqueue durable Sling jobs.
- The scheduler drains the JCR queue, builds documents, validates mandatory fields, and posts batches.
- Failures are retried with exponential backoff; persistent failures are dead-lettered and recorded in the Indexing Report.

**Full index:**

- Triggered from **Tools → SearchStax → Full Index** or programmatically via the full-index job topic.
- Traverses configured include paths, respects exclude paths and per-document size limits (10 KB default).
- Batch size, commit window, and traversal mode are tunable via OSGi (`SearchStaxFullIndexRuntimeConfiguration`).

### Maintenance mode (SRS 5.9)

Maintenance mode pauses incremental indexing and shows a message on the search component.

- **Manual:** Enable in **Initial Setup → Maintenance** tab.
- **Automatic:** Activates after N consecutive SearchStax 5xx responses (default threshold: 3).
- Successful 2xx responses reset the failure counter.

### Site search component (SRS 5.10)

Add the **SearchStax Search** component (`searchstaxconnector/components/searchstax-search`) to a page. It reads the select endpoint/token from API Configuration and optionally overrides the search profile (`Model` query param).

Requires select endpoint and token configured in **API Configuration**.

### Limits

| Limit | Value |
|-------|-------|
| Max document payload | 10 KB |
| Max batch payload | 10 MB |
| Max documents per batch | 100 |

Oversize documents are skipped and logged in the indexing audit.

### Logging

Dedicated log category: `com.searchstax.aem.connector`

## Documentation

- [SRS v1.0](docs/SRS.md) — Software Requirements Specification
- [Smoke & cross-platform tests](docs/SMOKE_TEST.md) — 6.5 / AMS / Cloud verification (traceability index)

## Legacy repository

The previous archetype-based repo (`aem-connector-repo`) is superseded by this project. Do not deploy site components or sample content from the old repo.
