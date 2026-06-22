package com.searchstax.aem.connector.core.constants;

/**
 * SRS-aligned size limits for incremental and full-index payloads.
 */
public final class SearchStaxIndexingLimits {

    /** SRS soft limit: 10 KB per document. */
    public static final int MAX_DOCUMENT_BYTES = 10 * 1024;

    /** SRS batch payload limit: 10 MB. */
    public static final int MAX_BATCH_PAYLOAD_BYTES = 10 * 1024 * 1024;

    /** Maximum documents per batch POST. */
    public static final int MAX_BATCH_DOCUMENT_COUNT = 100;

    /** Maximum retry attempts after the first indexing failure (incremental and full reindex). */
    public static final int MAX_INDEXING_RETRIES = 5;

    private SearchStaxIndexingLimits() {
    }
}
