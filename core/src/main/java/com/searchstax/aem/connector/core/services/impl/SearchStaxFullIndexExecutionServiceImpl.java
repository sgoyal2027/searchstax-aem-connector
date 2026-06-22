package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.SiteRoutingResult;
import com.searchstax.aem.connector.core.config.SearchStaxFullIndexRuntimeConfigService;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults.TraversalMode;
import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import com.searchstax.aem.connector.core.dto.SearchStaxUpdateOptions;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexProgress.State;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexDocumentBuilder;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexDocumentSerializer;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexExecutionService;
import com.searchstax.aem.connector.core.services.FullIndexAuditService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexFailureStore;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexPathConfigurationService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRetryPolicy;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexSerializedDocument;
import com.searchstax.aem.connector.core.services.SearchStaxIndexBatchBuffer;
import com.searchstax.aem.connector.core.services.SearchStaxIndexBatchEntry;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import com.searchstax.aem.connector.core.services.SiteRoutingService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Full-index execution. HTTP retry applies only to the batch currently being flushed (fresh attempt counter per
 * batch). Failed batches are cleared and not replayed later in the same run.
 */
@Component(service = SearchStaxFullIndexExecutionService.class)
public class SearchStaxFullIndexExecutionServiceImpl implements SearchStaxFullIndexExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxFullIndexExecutionServiceImpl.class);

    private static final int IO_RETRYABLE_STATUS = 599;

    /** HTTP status used in failure records for per-path payload limit (no batch retry). */
    private static final int PATH_PAYLOAD_LIMIT_STATUS = 413;

    private static final int PATH_SERIALIZE_FAILURE_STATUS = 422;

    private static final int PATH_RESOLVER_FAILURE_STATUS = 503;

    private static final int PATH_BUILD_FAILURE_STATUS = 500;

    private static final String FULL_INDEX_FAILURE_SUBJECT = "SearchStax Full Index Completed With Failures";

    private final SearchStaxFullIndexDocumentSerializer documentSerializer =
            new SearchStaxFullIndexDocumentSerializer();

    @Reference
    private ResolverUtil resolverUtil;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private SearchStaxFullIndexDocumentBuilder documentBuilder;

    @Reference
    private SearchStaxFullIndexPathConfigurationService pathConfigurationService;

    @Reference
    private SearchstaxClientService searchstaxClientService;

    @Reference
    private SiteRoutingService siteRoutingService;

    @Reference
    private SearchStaxFullIndexRuntimeConfigService runtimeConfigService;

    private final Object progressLock = new Object();
    private State state = State.IDLE;
    private long totalProcessed;
    private long successCount;
    private long failureCount;
    private int failedBatchCount;
    private long pagesIndexed;
    private long assetsIndexed;
    private int currentBatchNumber;
    private String lastIndexedPath = "";
    private long startedAt;
    private String progressMessage = "";

    private int batchesSinceHardCommit;
    private boolean lastBatchWasHardCommit;
    private String[] effectiveExcludes = new String[0];
    private Map<String, Boolean> includeChildPathMap = new java.util.HashMap<>();

    @Reference
    private SearchStaxFullIndexFailureStore failureStore;

    @Reference
    private FullIndexAuditService fullIndexAuditService;

    @Reference
    private EmailService emailService;

    @Reference
    private EmailConfigService emailConfigService;

    public SearchStaxFullIndexExecutionServiceImpl() {
        // SCR
    }

    SearchStaxFullIndexExecutionServiceImpl(final SearchStaxFullIndexFailureStore failureStore) {
        this.failureStore = failureStore;
    }

    SearchStaxFullIndexExecutionServiceImpl(
            final SearchStaxFullIndexFailureStore failureStore, final EmailService emailService) {
        this.failureStore = failureStore;
        this.emailService = emailService;
    }

    SearchStaxFullIndexExecutionServiceImpl(
            final SearchStaxFullIndexFailureStore failureStore,
            final EmailService emailService,
            final EmailConfigService emailConfigService) {
        this.failureStore = failureStore;
        this.emailService = emailService;
        this.emailConfigService = emailConfigService;
    }

    @Override
    public void execute() {
        execute(new FullIndexPathConfig("", new String[0], new boolean[0], new String[0]));
    }

    @Override
    public void execute(final FullIndexPathConfig pathConfig) {
        final FullIndexPathConfig config = pathConfig == null ? new FullIndexPathConfig("", new String[0], new boolean[0], new String[0]) : pathConfig;
        final String[] effectiveIncludes = pathConfigurationService.resolveEffectiveIncludes(config);
        effectiveExcludes = pathConfigurationService.resolveEffectiveExcludes(config);

        final boolean[] rawChildFlags = config.getIncludeChildPaths();
        includeChildPathMap = new java.util.HashMap<>();
        for (int i = 0; i < effectiveIncludes.length; i++) {
            includeChildPathMap.put(effectiveIncludes[i],
                    i < rawChildFlags.length && rawChildFlags[i]);
        }

        resetProgress(State.RUNNING, "Full index started");
        LOG.info(
                "Full index execution starting. traversalMode={}, root={}, includePaths={}, excludePaths={}, "
                        + "includeChildPaths={}, batchSize={}, maxBatchPayloadBytes={}",
                runtimeConfigService.getTraversalMode(),
                config.getRootPath(),
                effectiveIncludes,
                effectiveExcludes,
                includeChildPathMap,
                runtimeConfigService.getBatchSize(),
                SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES);

        if (effectiveIncludes.length == 0) {
            failProgress("No valid include paths under root");
            throw new IllegalStateException("No valid include paths under root: " + config.getRootPath());
        }

        batchesSinceHardCommit = 0;
        lastBatchWasHardCommit = false;

        final SearchStaxIndexBatchBuffer batch = new SearchStaxIndexBatchBuffer();

        try {
            runIndexingTraversalPhases(effectiveIncludes, includeChildPathMap, batch);

            if (!batch.isEmpty()) {
                flushBatch(batch, true, BatchFlushReason.FINAL_BATCH);
            } else if (runtimeConfigService.isHardCommitOnSuccess() && !lastBatchWasHardCommit) {
                final BatchPostResult commitResult = postBatchWithRetry("[]", true, 0, List.of(), 2);
                if (!commitResult.isSuccess()) {
                    incrementFailedBatchCount();
                    LOG.error(
                            "Final hard commit failed with HTTP {}; continuing to finish run",
                            commitResult.statusCode);
                } else {
                    lastBatchWasHardCommit = true;
                }
            }

            finishProgress();
            sendConsolidatedFailureEmailIfNeeded();
            LOG.info(
                    "Full index finished state={}. totalProcessed={}, success={}, pathFailures={}, "
                            + "failedBatches={}, pages={}, assets={}, batches={}",
                    state,
                    totalProcessed,
                    successCount,
                    failureCount,
                    failedBatchCount,
                    pagesIndexed,
                    assetsIndexed,
                    currentBatchNumber);

        } catch (final Exception e) {
            LOG.error("Full index execution failed", e);
            failProgress("Full index failed: " + e.getMessage());
            throw new IllegalStateException("Full index failed", e);
        }
    }

    @Override
    public FullIndexProgress getProgressSnapshot() {
        synchronized (progressLock) {
            final long elapsed = startedAt > 0 ? System.currentTimeMillis() - startedAt : 0;
            return new FullIndexProgress(
                    state,
                    totalProcessed,
                    successCount,
                    failureCount,
                    pagesIndexed,
                    assetsIndexed,
                    currentBatchNumber,
                    lastIndexedPath,
                    startedAt,
                    elapsed,
                    progressMessage);
        }
    }

    /**
     * Phase 1: content includes (pages). Phase 2a: referenced DAM assets from indexed pages. Phase 2b: explicit DAM
     * includes. Package-visible for unit tests.
     */
    void runIndexingTraversalPhases(
            final String[] effectiveIncludes,
            final Map<String, Boolean> includeChildPaths,
            final SearchStaxIndexBatchBuffer batch) {
        final String[] contentIncludes = splitContentIncludes(effectiveIncludes);
        final String[] damIncludes = splitDamIncludes(effectiveIncludes);
        final Set<String> referencedAssets = ConcurrentHashMap.newKeySet();
        final Set<String> processedAssetPaths = ConcurrentHashMap.newKeySet();

        for (final String includeRoot : contentIncludes) {
            if (includeRoot == null || includeRoot.isEmpty()) {
                continue;
            }
            final boolean recursive = includeChildPaths.getOrDefault(includeRoot, false);
            LOG.info("Processing content include path: {} (includeChildPath={})", includeRoot, recursive);
            forEachPagePath(
                    includeRoot,
                    recursive,
                    path -> collectPath(path, true, batch, referencedAssets, processedAssetPaths));
        }

        final int batchSizeBeforeAssets;
        final int pagesIndexedBeforeAssets;
        synchronized (progressLock) {
            batchSizeBeforeAssets = batch.size();
            pagesIndexedBeforeAssets = (int) pagesIndexed;
            progressMessage = "Indexing referenced assets...";
        }
        LOG.info(
                "Referenced asset indexing starting (referencedCount={}, pagesIndexed={}, currentBatchNumber={}, "
                        + "batchSize={})",
                referencedAssets.size(),
                pagesIndexedBeforeAssets,
                currentBatchNumber,
                batchSizeBeforeAssets);
        if (!referencedAssets.isEmpty()) {
            final String[] sortedReferences = referencedAssets.toArray(new String[0]);
            Arrays.sort(sortedReferences);
            for (final String assetPath : sortedReferences) {
                collectAssetPathIfNotProcessed(assetPath, batch, processedAssetPaths);
            }
        }

        synchronized (progressLock) {
            progressMessage = "Indexing DAM include paths...";
        }
        LOG.info(
                "DAM include traversal starting (damIncludeCount={}, processedAssetCount={}, batchSize={})",
                damIncludes.length,
                processedAssetPaths.size(),
                batch.size());
        for (final String damInclude : damIncludes) {
            if (damInclude == null || damInclude.isEmpty()) {
                continue;
            }
            final boolean damRecursive = includeChildPaths.getOrDefault(damInclude, false);
            LOG.info("Processing DAM include path: {} (includeChildPath={})", damInclude, damRecursive);
            forEachAssetPath(
                    damInclude,
                    damRecursive,
                    path -> collectAssetPathIfNotProcessed(path, batch, processedAssetPaths));
        }
    }

    private static String[] splitContentIncludes(final String[] effectiveIncludes) {
        if (effectiveIncludes == null || effectiveIncludes.length == 0) {
            return new String[0];
        }
        final List<String> content = new ArrayList<>();
        for (final String include : effectiveIncludes) {
            if (isContentInclude(include)) {
                content.add(include);
            }
        }
        return content.toArray(new String[0]);
    }

    private static String[] splitDamIncludes(final String[] effectiveIncludes) {
        if (effectiveIncludes == null || effectiveIncludes.length == 0) {
            return new String[0];
        }
        final List<String> dam = new ArrayList<>();
        for (final String include : effectiveIncludes) {
            if (isDamInclude(include)) {
                dam.add(include);
            }
        }
        return dam.toArray(new String[0]);
    }

    private static boolean isContentInclude(final String path) {
        return path != null
                && !path.isEmpty()
                && path.startsWith("/content")
                && !path.startsWith(SearchStaxFullIndexDefaults.DAM_ROOT);
    }

    private static boolean isDamInclude(final String path) {
        return path != null && !path.isEmpty() && path.startsWith(SearchStaxFullIndexDefaults.DAM_ROOT);
    }

    void collectAssetPathIfNotProcessed(
            final String path,
            final SearchStaxIndexBatchBuffer batch,
            final Set<String> processedAssetPaths) {
        if (path == null || path.isEmpty() || processedAssetPaths.contains(path)) {
            return;
        }
        collectPath(path, false, batch, null, processedAssetPaths);
    }

    void forEachPagePath(
            final String includeRoot, final boolean recursive, final Consumer<String> consumer) {
        if (runtimeConfigService.getTraversalMode() == TraversalMode.QUERY_BUILDER) {
            forEachPathsKeysetQueryBuilder("cq:Page", includeRoot, recursive, consumer);
        } else {
            forEachPathsKeysetSql2(false, includeRoot, recursive, consumer);
        }
    }

    void forEachAssetPath(
            final String includeRoot, final boolean recursive, final Consumer<String> consumer) {
        if (runtimeConfigService.getTraversalMode() == TraversalMode.QUERY_BUILDER) {
            forEachPathsKeysetQueryBuilder("dam:Asset", includeRoot, recursive, consumer);
        } else {
            forEachPathsKeysetSql2(true, includeRoot, recursive, consumer);
        }
    }

    private void forEachPathsKeysetQueryBuilder(
            final String nodeType,
            final String includeRoot,
            final boolean recursive,
            final Consumer<String> consumer) {
        String lastPath = "";
        while (true) {
            final List<String> paths = queryPathsQueryBuilder(nodeType, includeRoot, recursive, lastPath);
            if (paths.isEmpty()) {
                break;
            }
            paths.forEach(consumer);
            lastPath = paths.get(paths.size() - 1);
            if (paths.size() < runtimeConfigService.getQueryPageSize()) {
                break;
            }
        }
    }

    private List<String> queryPathsQueryBuilder(
            final String nodeType,
            final String includeRoot,
            final boolean recursive,
            final String lastPath) {
        final List<String> paths = new ArrayList<>();
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new IllegalStateException("Could not adapt to JCR Session");
            }
            final Map<String, String> predicates = new HashMap<>();
            predicates.put("type", nodeType);
            predicates.put("path", includeRoot);
            predicates.put("path.self", recursive ? "false" : "true");
            predicates.put("p.limit", String.valueOf(runtimeConfigService.getQueryPageSize()));
            predicates.put("orderby", "@jcr:path");
            predicates.put("orderby.sort", "asc");
            if (lastPath != null && !lastPath.isEmpty()) {
                predicates.put("property", "jcr:path");
                predicates.put("property.operation", "greater");
                predicates.put("property.value", lastPath);
            }
            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicates), session);
            final SearchResult result = query.getResult();
            for (final Hit hit : result.getHits()) {
                try {
                    paths.add(hit.getPath());
                } catch (final RepositoryException e) {
                    LOG.warn("Could not read hit path", e);
                }
            }
        } catch (final LoginException e) {
            throw new IllegalStateException("Could not obtain service resolver", e);
        }
        return paths;
    }

    private void forEachPathsKeysetSql2(
            final boolean asset,
            final String includeRoot,
            final boolean recursive,
            final Consumer<String> consumer) {
        String lastPath = "";
        int chunkCount = 0;
        while (true) {
            final List<String> paths = queryPathsSql2(asset, includeRoot, recursive, lastPath);
            if (paths.isEmpty()) {
                break;
            }
            paths.forEach(consumer);
            lastPath = paths.get(paths.size() - 1);
            chunkCount++;
            if (paths.size() < runtimeConfigService.getQueryPageSize()) {
                break;
            }
            if (chunkCount % runtimeConfigService.getResolverRefreshEveryNBatches() == 0) {
                refreshResolver();
            }
        }
    }

    private List<String> queryPathsSql2(
            final boolean asset, final String includeRoot, final boolean recursive, final String lastPath) {
        final List<String> paths = new ArrayList<>();
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new IllegalStateException("Could not adapt to JCR Session");
            }
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final javax.jcr.query.Query query =
                    queryManager.createQuery(buildSql2(asset, includeRoot, recursive, lastPath), "JCR-SQL2");
            query.setLimit(runtimeConfigService.getQueryPageSize());
            final QueryResult result = query.execute();
            final RowIterator rows = result.getRows();
            while (rows.hasNext()) {
                final Row row = rows.nextRow();
                paths.add(row.getValue("jcr:path").getString());
            }
        } catch (final LoginException | RepositoryException e) {
            throw new IllegalStateException("JCR-SQL2 query failed for root " + includeRoot, e);
        }
        return paths;
    }

    private static String buildSql2(
            final boolean asset, final String includeRoot, final boolean recursive, final String lastPath) {
        final String nodeType = asset ? "dam:Asset" : "cq:Page";
        final String alias = asset ? "asset" : "page";
        final String escapedRoot = escapeSql(includeRoot);
        final String pathGreater =
                lastPath.isEmpty() ? "" : String.format("AND %s.[jcr:path] > '%s' ", alias, escapeSql(lastPath));
        final String pathScope =
                recursive
                        ? String.format(
                                "(ISDESCENDANTNODE(%s, '%s') OR %s.[jcr:path] = '%s')",
                                alias, escapedRoot, alias, escapedRoot)
                        : String.format("%s.[jcr:path] = '%s'", alias, escapedRoot);
        return String.format(
                "SELECT %s.[jcr:path] FROM [%s] AS %s WHERE %s %s ORDER BY %s.[jcr:path]",
                alias, nodeType, alias, pathScope, pathGreater, alias);
    }

    private void refreshResolver() {
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            resolver.refresh();
        } catch (final LoginException e) {
            LOG.debug("Could not refresh resolver", e);
        }
    }

    private static String escapeSql(final String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    void collectPath(
            final String path,
            final boolean page,
            final SearchStaxIndexBatchBuffer batch,
            final Set<String> referencedAssets,
            final Set<String> processedAssetPaths) {
        if (isExcluded(path)) {
            LOG.debug("Excluded path: {}", path);
            return;
        }
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Map<String, Object> document =
                    page
                            ? documentBuilder.buildPageIfPublished(resolver, path)
                            : documentBuilder.buildAssetIfPublished(resolver, path);
            if (document == null) {
                return;
            }
            final Optional<SearchStaxFullIndexSerializedDocument> serialized = documentSerializer.serialize(document);
            if (serialized.isEmpty()) {
                LOG.warn("Failed to serialize document for path {}", path);
                recordPathFailure(
                        path,
                        "serialize",
                        "Failed to serialize document",
                        0,
                        PATH_SERIALIZE_FAILURE_STATUS);
                return;
            }
            final SearchStaxFullIndexSerializedDocument serializedDocument = serialized.get();
            final int documentBytes = serializedDocument.getBytes();
            final String documentJson = serializedDocument.getJson();

            if (!documentSerializer.isWithinDocumentLimit(documentBytes)) {
                final String errorMessage =
                        String.format(
                                "Document payload %d bytes exceeds SRS %d byte limit",
                                documentBytes,
                                SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES);
                LOG.warn("Skipping path {}: {}", path, errorMessage);
                recordPathFailure(path, "document-limit", errorMessage, documentBytes, PATH_PAYLOAD_LIMIT_STATUS);
                return;
            }

            if (batch.wouldExceed(documentBytes) && !batch.isEmpty()) {
                flushBatch(batch, false, BatchFlushReason.PAYLOAD_SIZE_LIMIT);
            }
            if (batch.wouldExceed(documentBytes)) {
                final String errorMessage =
                        String.format(
                                "Single document payload %d bytes exceeds max batch payload %d bytes",
                                documentBytes,
                                SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES);
                LOG.error("Skipping path {}: {}", path, errorMessage);
                recordPathFailure(
                        path, "payload-limit", errorMessage, documentBytes, PATH_PAYLOAD_LIMIT_STATUS);
                return;
            }

            batch.add(new SearchStaxIndexBatchEntry(document, path, page, documentJson, documentBytes));
            synchronized (progressLock) {
                lastIndexedPath = path;
                if (page) {
                    pagesIndexed++;
                } else {
                    assetsIndexed++;
                }
            }
            if (page && referencedAssets != null) {
                referencedAssets.addAll(documentBuilder.collectDamReferencesFromPage(resolver, path));
            } else if (!page && processedAssetPaths != null) {
                processedAssetPaths.add(path);
            }
            if (batch.size() >= runtimeConfigService.getBatchSize()) {
                flushBatch(batch, false, BatchFlushReason.DOCUMENT_COUNT_LIMIT);
            }
        } catch (final LoginException e) {
            LOG.warn("Resolver error for path {}", path, e);
            recordPathFailure(
                    path,
                    "resolver",
                    "Resolver error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    0,
                    PATH_RESOLVER_FAILURE_STATUS);
        } catch (final IOException e) {
            throw new IllegalStateException("Batch flush interrupted for path " + path, e);
        } catch (final Exception e) {
            LOG.warn("Failed to build or queue document for path {}", path, e);
            recordPathFailure(
                    path,
                    "build",
                    "Failed to build or queue document: "
                            + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    0,
                    PATH_BUILD_FAILURE_STATUS);
        }
    }

    /**
     * @return true if the batch was indexed successfully, false if POST failed after retries (batch cleared either way)
     */
    private boolean flushBatch(
            final SearchStaxIndexBatchBuffer batch, final boolean finalBatch, final BatchFlushReason reason)
            throws IOException {
        if (batch.isEmpty()) {
            return true;
        }
        LOG.debug(
                "Flushing full-index batch: reason={}, docs={}, payloadBytes={}",
                reason,
                batch.size(),
                batch.getPayloadBytes());

        final int batchNum;
        synchronized (progressLock) {
            currentBatchNumber++;
            batchNum = currentBatchNumber;
        }

        final boolean hardCommit =
                finalBatch
                        || batchesSinceHardCommit >= runtimeConfigService.getHardCommitEveryNBatches();

        final long batchStart = System.currentTimeMillis();
        final String json = batch.toJson();
        final int payloadBytes = batch.getPayloadBytes();
        final List<String> paths = extractPaths(batch);

        final BatchPostResult result =
                postBatchWithRetry(json, hardCommit, batchNum, paths, payloadBytes);
        final long duration = System.currentTimeMillis() - batchStart;

        if (!result.isSuccess()) {
            incrementFailedBatchCount();
            LOG.error(
                    "Batch {} failed with HTTP {}; clearing batch and continuing traversal",
                    batchNum,
                    result.statusCode);
            batch.clear();
            return false;
        }

        final int size = batch.size();
        synchronized (progressLock) {
            successCount += size;
            totalProcessed += size;
        }
        if (hardCommit) {
            batchesSinceHardCommit = 0;
            lastBatchWasHardCommit = true;
        } else {
            batchesSinceHardCommit++;
            lastBatchWasHardCommit = false;
        }

        final double docsPerSec = duration > 0 ? (size * 1000.0) / duration : size;
        LOG.info(
                "Batch {} complete: size={}, hardCommit={}, durationMs={}, docsPerSec={}, totalProcessed={}, "
                        + "success={}, failures={}, lastPath={}",
                batchNum,
                size,
                hardCommit,
                duration,
                String.format("%.2f", docsPerSec),
                totalProcessed,
                successCount,
                failureCount,
                lastIndexedPath);

        recordSuccessAudit(paths, batchNum, duration);

        batch.clear();
        throttle();
        return true;
    }

    private void recordSuccessAudit(final List<String> paths, final int batchNum, final long durationMs) {
        if (fullIndexAuditService == null || paths == null || paths.isEmpty()) {
            return;
        }
        fullIndexAuditService.recordSuccessBatch(paths, batchNum, durationMs);
    }

    private static List<String> extractPaths(final SearchStaxIndexBatchBuffer batch) {
        final List<String> paths = new ArrayList<>(batch.size());
        for (final SearchStaxIndexBatchEntry entry : batch.getEntries()) {
            paths.add(entry.getPath() != null ? entry.getPath() : "");
        }
        return paths;
    }

    private enum BatchFlushReason {
        PAYLOAD_SIZE_LIMIT,
        DOCUMENT_COUNT_LIMIT,
        FINAL_BATCH
    }

    private BatchPostResult postBatchWithRetry(
            final String requestJson,
            final boolean hardCommit,
            final int batchNum,
            final List<String> paths,
            final int payloadBytes)
            throws IOException {
        BatchPostResult lastResult = null;
        for (int attempt = 1; attempt <= SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS; attempt++) {
            try {
                final String routingPath = paths.isEmpty() ? "" : paths.get(0);
                lastResult = postBatchOnce(requestJson, hardCommit, routingPath);
            } catch (final IOException e) {
                lastResult = new BatchPostResult(IO_RETRYABLE_STATUS, e.getMessage());
                LOG.warn(
                        "Batch {} transport error on attempt {}/{}: {}",
                        batchNum,
                        attempt,
                        SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS,
                        e.getMessage());
            }
            if (lastResult.isSuccess()) {
                return lastResult;
            }

            final int status = lastResult.statusCode;
            final boolean retryable =
                    status == IO_RETRYABLE_STATUS || SearchStaxFullIndexRetryPolicy.isRetryable(status);
            if (SearchStaxFullIndexRetryPolicy.isNonRetryable(status) || !retryable) {
                LOG.error(
                        "Batch {} non-retryable HTTP {} after attempt {}/{}",
                        batchNum,
                        status,
                        attempt,
                        SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS);
                recordBatchFailure(batchNum, paths, lastResult, payloadBytes, attempt);
                return lastResult;
            }

            if (attempt >= SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS) {
                recordBatchFailure(batchNum, paths, lastResult, payloadBytes, attempt);
                return lastResult;
            }

            final long delayMs = SearchStaxFullIndexRetryPolicy.backoffMillis(attempt);
            LOG.warn(
                    "Batch {} retryable HTTP {} on attempt {}/{}; retrying after {} ms",
                    batchNum,
                    status,
                    attempt,
                    SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS,
                    delayMs);
            sleepBackoff(delayMs);
        }
        return lastResult;
    }

    private void recordBatchFailure(
            final int batchNum,
            final List<String> paths,
            final BatchPostResult result,
            final int payloadBytes,
            final int attempts) {
        try {
            failureStore.recordFailure(
                    new SearchStaxFullIndexFailureStore.FailureRecord(
                            "batch-" + batchNum,
                            paths,
                            result.statusCode,
                            result.errorMessage,
                            payloadBytes,
                            Instant.now(),
                            attempts));
            LOG.error(
                    "Batch {} failed with HTTP {} after {} attempt(s); failure recorded",
                    batchNum,
                    result.statusCode,
                    attempts);
        } catch (final IOException e) {
            LOG.error(
                    "Batch {} failed with HTTP {} after {} attempt(s); could not persist failure record",
                    batchNum,
                    result.statusCode,
                    attempts,
                    e);
        }
    }

    BatchPostResult postBatchOnce(final String requestJson, final boolean hardCommit) throws IOException {
        return postBatchOnce(requestJson, hardCommit, "");
    }

    BatchPostResult postBatchOnce(
            final String requestJson, final boolean hardCommit, final String contentPath) throws IOException {

        SearchStaxUpdateOptions options =
                SearchStaxUpdateOptions.fullIndexBatch(hardCommit, runtimeConfigService.getCommitWithinMs());
        if (contentPath != null && !contentPath.isBlank()) {
            final SiteRoutingResult routing = siteRoutingService.resolve(contentPath);
            options = SearchStaxUpdateOptions.routed(
                    hardCommit,
                    runtimeConfigService.getCommitWithinMs(),
                    routing.getUpdateEndpoint(),
                    routing.getUpdateToken(),
                    routing.getSearchProfile());
        }

        final ApiResponse response = searchstaxClientService.postUpdate(requestJson, options);
        if (response.getStatusCode() == SearchstaxClientServiceImpl.TRANSPORT_ERROR_STATUS) {
            throw new IOException(response.getResponseBody());
        }

        final String body = response.getResponseBody() != null ? response.getResponseBody() : "";
        if (response.getStatusCode() >= 400) {
            LOG.error("SearchStax batch update failed with HTTP {}: {}", response.getStatusCode(), body);
        }
        return new BatchPostResult(response.getStatusCode(), body);
    }

    void sleepBackoff(final long delayMs) throws IOException {
        try {
            Thread.sleep(delayMs);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Full index batch retry interrupted during backoff", e);
        }
    }

    static final class BatchPostResult {
        final int statusCode;
        final String errorMessage;

        BatchPostResult(final int statusCode, final String errorMessage) {
            this.statusCode = statusCode;
            this.errorMessage = errorMessage;
        }

        boolean isSuccess() {
            return statusCode >= 200 && statusCode < 400;
        }
    }

    private void throttle() {
        try {
            Thread.sleep(runtimeConfigService.getBatchThrottleMs());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isExcluded(final String path) {
        if (pathConfigurationService == null) {
            return false;
        }
        return pathConfigurationService.isExcludedPath(path, effectiveExcludes);
    }

    private void incrementFailure() {
        synchronized (progressLock) {
            failureCount++;
        }
    }

    /**
     * Persists a per-path failure immediately (no HTTP retry). Used for serialize errors, payload limits, and build
     * failures.
     */
    private void recordPathFailure(
            final String path,
            final String failureKind,
            final String errorMessage,
            final int payloadBytes,
            final int statusCode) {
        incrementFailure();
        if (failureStore == null) {
            LOG.warn("Path failure for {} ({}); FailureStore unavailable", path, errorMessage);
            return;
        }
        try {
            final String batchId = "path-" + failureKind + "-" + sanitizePathForFailureId(path);
            failureStore.recordFailure(
                    new SearchStaxFullIndexFailureStore.FailureRecord(
                            batchId,
                            List.of(path),
                            statusCode,
                            errorMessage,
                            payloadBytes,
                            Instant.now(),
                            0));
            LOG.info("Full index path failure recorded for {} ({})", path, failureKind);
        } catch (final IOException e) {
            LOG.error("Could not persist path failure for {}", path, e);
        }
    }

    private static String sanitizePathForFailureId(final String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        final String sanitized = path.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.length() <= 120) {
            return sanitized;
        }
        return sanitized.substring(0, 120);
    }

    private void incrementFailedBatchCount() {
        synchronized (progressLock) {
            failedBatchCount++;
        }
    }

    private void resetProgress(final State newState, final String message) {
        synchronized (progressLock) {
            state = newState;
            totalProcessed = 0;
            successCount = 0;
            failureCount = 0;
            failedBatchCount = 0;
            pagesIndexed = 0;
            assetsIndexed = 0;
            currentBatchNumber = 0;
            lastIndexedPath = "";
            startedAt = System.currentTimeMillis();
            progressMessage = message;
        }
    }

    private void finishProgress() {
        synchronized (progressLock) {
            if (failedBatchCount == 0 && failureCount == 0) {
                state = State.SUCCESS;
                progressMessage = "Full index completed successfully";
            } else if (failedBatchCount == 0) {
                state = State.PARTIAL_FAILURE;
                progressMessage =
                        "Full index completed with " + failureCount + " path failure(s)";
            } else if (failureCount == 0) {
                state = State.PARTIAL_FAILURE;
                progressMessage =
                        "Full index completed with " + failedBatchCount + " failed batch(es)";
            } else {
                state = State.PARTIAL_FAILURE;
                progressMessage =
                        "Full index completed with "
                                + failedBatchCount
                                + " failed batch(es) and "
                                + failureCount
                                + " path failure(s)";
            }
        }
    }

    private void failProgress(final String message) {
        synchronized (progressLock) {
            state = State.FAILED;
            progressMessage = message;
        }
    }

    private void sendConsolidatedFailureEmailIfNeeded() {
        final int batchesFailed;
        final long pathFailures;
        final long runStartedAt;
        synchronized (progressLock) {
            batchesFailed = failedBatchCount;
            pathFailures = failureCount;
            runStartedAt = startedAt;
        }
        if (batchesFailed <= 0 && pathFailures <= 0) {
            return;
        }
        if (emailService == null) {
            LOG.warn(
                    "Full index completed with {} failed batch(es) and {} path failure(s) but EmailService is unavailable",
                    batchesFailed,
                    pathFailures);
            return;
        }
        final String[] recipients = emailConfigService.getReceiverAddresses();
        if (recipients.length == 0) {
            LOG.warn(
                    "Full index completed with {} failed batch(es) and {} path failure(s) but no recipients configured",
                    batchesFailed,
                    pathFailures);
            return;
        }
        try {
            final List<SearchStaxFullIndexFailureStore.FailureRecord> failures =
                    failureStore.listFailuresSince(Instant.ofEpochMilli(runStartedAt));
            final FullIndexProgress progress = getProgressSnapshot();
            final String body = buildFullIndexFailureEmailBody(progress, batchesFailed, failures);

            final EmailRequest request = new EmailRequest();
            request.setSubject(FULL_INDEX_FAILURE_SUBJECT);
            request.setBody(body);
            request.setRecipients(recipients);
            emailService.sendEmail(request);
            LOG.info(
                    "Full index consolidated failure email sent: failedBatches={}, pathFailures={}, persistedFailures={}",
                    batchesFailed,
                    pathFailures,
                    failures.size());
        } catch (final Exception e) {
            LOG.error("Failed to send consolidated full index failure email", e);
        }
    }

    private static String buildFullIndexFailureEmailBody(
            final FullIndexProgress progress,
            final int failedBatches,
            final List<SearchStaxFullIndexFailureStore.FailureRecord> failures) {
        final StringBuilder body = new StringBuilder();
        body.append("<h3>SearchStax Full Index Completed With Failures</h3>");
        body.append("<p><b>State:</b> ").append(progress.getState()).append("</p>");
        body.append("<p><b>Message:</b> ").append(escapeHtml(progress.getMessage())).append("</p>");
        body.append("<p><b>Elapsed (ms):</b> ").append(progress.getElapsedMs()).append("</p>");
        body.append("<p><b>Total Processed:</b> ").append(progress.getTotalProcessed()).append("</p>");
        body.append("<p><b>Success Count:</b> ").append(progress.getSuccessCount()).append("</p>");
        body.append("<p><b>Path Failures:</b> ").append(progress.getFailureCount()).append("</p>");
        body.append("<p><b>Failed Batches:</b> ").append(failedBatches).append("</p>");
        body.append("<p><b>Pages Indexed:</b> ").append(progress.getPagesIndexed()).append("</p>");
        body.append("<p><b>Assets Indexed:</b> ").append(progress.getAssetsIndexed()).append("</p>");
        body.append("<p><b>Batches Flushed:</b> ").append(progress.getCurrentBatchNumber()).append("</p>");

        body.append("<h4>Failure Details</h4>");
        if (failures.isEmpty()) {
            body.append("<p>No failure records found under var storage for this run.</p>");
        } else {
            for (final SearchStaxFullIndexFailureStore.FailureRecord failure : failures) {
                body.append("<hr/>");
                final boolean pathFailure =
                        failure.getBatchId() != null && failure.getBatchId().startsWith("path-");
                body.append("<p><b>")
                        .append(pathFailure ? "Failure ID" : "Batch ID")
                        .append(":</b> ")
                        .append(escapeHtml(failure.getBatchId()))
                        .append("</p>");
                body.append("<p><b>Type:</b> ")
                        .append(pathFailure ? "Path (no retry)" : "Batch POST")
                        .append("</p>");
                body.append("<p><b>Status Code:</b> ").append(failure.getStatusCode()).append("</p>");
                if (!pathFailure) {
                    body.append("<p><b>Retry Attempts:</b> ").append(failure.getRetryAttempts()).append("</p>");
                }
                body.append("<p><b>Timestamp:</b> ").append(escapeHtml(failure.getTimestamp())).append("</p>");
                body.append("<p><b>Error:</b></p><pre>").append(escapeHtml(failure.getErrorMessage())).append("</pre>");
                body.append("<p><b>Paths:</b></p><pre>");
                final List<String> paths = failure.getPaths();
                if (paths == null || paths.isEmpty()) {
                    body.append("(none)");
                } else {
                    for (final String path : paths) {
                        body.append(escapeHtml(path)).append('\n');
                    }
                }
                body.append("</pre>");
            }
        }
        return body.toString();
    }

    private static String escapeHtml(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
