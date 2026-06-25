# SearchStax AEM Connector — SRS v1.0

Traceability index for BV-SS-AEM-2026. Place the approved SRS PDF in project documentation for full requirements text.

## Implemented modules

| SRS section | Feature | Status |
|-------------|---------|--------|
| 5.2 / 5.3 | Page and asset document builders | Implemented |
| 5.2.2 | Mandatory field validation | Implemented (backend + wizard UI) |
| 5.4.2 | JCR-backed incremental queue | Implemented |
| 5.6.1 | Site-to-application routing | Implemented (multifield wizard) |
| 5.6.2 | Search profile (`Model` param) | Implemented |
| 5.7.2 | Unified API configuration | Implemented |
| 5.8.1 | Incremental indexing service + backoff | Implemented |
| 5.8.3 / 5.11.1 | Indexing report + reprocess | Implemented |
| 5.10 | UX Toolkit search component | Minimal component (select API) |
| 3.3 | Language mapping (AEM → SearchStax suffix) | Implemented |
| 5.11.1 | Correlation IDs in indexing audit | Implemented |

## Architecture

```
Replication → Sling Job (durable) → JCR Pending Queue → Scheduler
  → IncrementalIndexingService → Document Builders → SearchstaxClientService
  → SearchStax update API (site routing + Model param)
Failures → dead-letter JCR + IndexingAuditService → Indexing Report UI
```

## Configuration paths

| Path | Purpose |
|------|---------|
| `/conf/searchstaxconnector/settings/initialsetup` | Connector enable, paths |
| `/conf/searchstaxconnector/settings/apiconfig` | Endpoints and tokens |
| `/conf/searchstaxconnector/settings/metadatafieldmapping` | Field mappings |
| `/var/searchstaxconnector/incremental-index/pending` | Incremental queue |
| `/var/searchstaxconnector/incremental-index/audit` | Indexing audit events |

See [README](../README.md) for operator setup and wizard usage.
