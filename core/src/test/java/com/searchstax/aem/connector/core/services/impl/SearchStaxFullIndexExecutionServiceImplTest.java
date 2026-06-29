package com.searchstax.aem.connector.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.SearchStaxFullIndexRuntimeConfigService;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults.TraversalMode;
import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexFailureStore;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRetryPolicy;
import com.searchstax.aem.connector.core.services.SearchStaxIndexBatchBuffer;
import com.searchstax.aem.connector.core.services.SearchStaxIndexBatchEntry;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexDocumentBuilder;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxFullIndexExecutionServiceImplTest {

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private SearchStaxFullIndexDocumentBuilder documentBuilder;

    @Mock
    private EmailConfigService emailConfigService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path failureDir;

    @Test
    void getProgressSnapshot_returnsCurrentCounters() throws Exception {
        final TestableExecutionService service = new TestableExecutionService(failureDir);
        setField(service, "state", FullIndexProgress.State.RUNNING);
        setField(service, "totalProcessed", 12L);
        setField(service, "successCount", 10L);
        setField(service, "failureCount", 2L);
        setField(service, "pagesIndexed", 8L);
        setField(service, "assetsIndexed", 4L);
        setField(service, "currentBatchNumber", 5);
        setField(service, "lastIndexedPath", "/content/wknd/en/page");
        setField(service, "startedAt", System.currentTimeMillis() - 250L);
        setField(service, "progressMessage", "Indexing");

        final FullIndexProgress snapshot = service.getProgressSnapshot();

        assertEquals(FullIndexProgress.State.RUNNING, snapshot.getState());
        assertEquals(12, snapshot.getTotalProcessed());
        assertEquals(10, snapshot.getSuccessCount());
        assertEquals(2, snapshot.getFailureCount());
        assertEquals(12, snapshot.getTotalAttempted());
        assertEquals(8, snapshot.getPagesIndexed());
        assertEquals(4, snapshot.getAssetsIndexed());
        assertEquals(5, snapshot.getCurrentBatchNumber());
        assertEquals("/content/wknd/en/page", snapshot.getLastIndexedPath());
        assertEquals("Indexing", snapshot.getMessage());
        assertTrue(snapshot.getElapsedMs() >= 0);
    }

    @Test
    void postBatchWithRetry_succeedsAfter503Retries() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                calls.incrementAndGet();
                if (calls.get() < 5) {
                    return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(503, "unavailable");
                }
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(200, "");
            }
        };

        final String json = "[{\"id\":\"/content/a\"}]";
        final SearchStaxFullIndexExecutionServiceImpl.BatchPostResult result =
                invokePostBatchWithRetry(service, json, false, 1, List.of("/content/a"), json.length());

        assertTrue(result.isSuccess());
        assertEquals(5, calls.get());
        assertEquals(0, countFailureFiles());
    }

    @Test
    void postBatchWithRetry_exhausted429PersistsFailure() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                calls.incrementAndGet();
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(429, "Too Many Requests");
            }
        };

        final String json = "[{\"id\":\"/content/x\"}]";
        final SearchStaxFullIndexExecutionServiceImpl.BatchPostResult result =
                invokePostBatchWithRetry(service, json, false, 3, List.of("/content/x"), json.length());

        assertFalse(result.isSuccess());
        assertEquals(429, result.statusCode);
        assertEquals(SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS, calls.get());
        assertEquals(1, countFailureFiles());
    }

    @Test
    void postBatchWithRetry_nonRetryable400PersistsFailure() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                calls.incrementAndGet();
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(400, "bad request");
            }
        };

        final SearchStaxFullIndexExecutionServiceImpl.BatchPostResult result =
                invokePostBatchWithRetry(service, "[]", false, 2, List.of(), 2);

        assertFalse(result.isSuccess());
        assertEquals(1, calls.get());
        assertEquals(1, countFailureFiles());
    }

    @Test
    void postBatchWithRetry_retriesOnIOExceptionThenSucceeds() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) throws IOException {
                calls.incrementAndGet();
                if (calls.get() < 3) {
                    throw new IOException("Error writing to server");
                }
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(200, "");
            }
        };

        final SearchStaxFullIndexExecutionServiceImpl.BatchPostResult result =
                invokePostBatchWithRetry(service, "[]", false, 2, List.of("/content/io"), 2);

        assertTrue(result.isSuccess());
        assertEquals(3, calls.get());
        assertEquals(0, countFailureFiles());
    }

    @Test
    void postBatchWithRetry_exhaustedIOExceptionPersistsFailure() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) throws IOException {
                calls.incrementAndGet();
                throw new IOException("Error writing to server");
            }
        };

        final SearchStaxFullIndexExecutionServiceImpl.BatchPostResult result =
                invokePostBatchWithRetry(service, "[]", false, 2, List.of("/content/io"), 2);

        assertFalse(result.isSuccess());
        assertEquals(599, result.statusCode);
        assertEquals(SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS, calls.get());
        assertEquals(1, countFailureFiles());
    }

    @Test
    void flushBatch_doesNotThrowWhenPostFails_clearsBatchIncrementsFailedBatchCount() throws Exception {
        final SearchStaxIndexBatchBuffer batch = bufferWithId("/content/fail");

        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(401, "Unauthorized");
            }
        };

        final boolean ok = invokeFlushBatch(service, batch, false, "DOCUMENT_COUNT_LIMIT");

        assertFalse(ok);
        assertTrue(batch.isEmpty());
        assertEquals(1, countFailureFiles());
        assertEquals(1, getFailedBatchCount(service));
    }

    @Test
    void flushBatch_twoBatches_firstFailsSecondSucceeds_noReplayOfFirstBatch() throws Exception {
        final SearchStaxIndexBatchBuffer batch1 = bufferWithId("/content/one");
        final SearchStaxIndexBatchBuffer batch2 = bufferWithId("/content/two");
        final List<String> payloads = new ArrayList<>();

        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                payloads.add(requestJson);
                if (requestJson.contains("/content/one")) {
                    return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(429, "limited");
                }
                if (requestJson.contains("/content/two")) {
                    return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(200, "");
                }
                throw new AssertionError("unexpected payload: " + requestJson);
            }
        };

        assertFalse(invokeFlushBatch(service, batch1, false, "DOCUMENT_COUNT_LIMIT"));
        assertTrue(batch1.isEmpty());
        assertTrue(invokeFlushBatch(service, batch2, false, "DOCUMENT_COUNT_LIMIT"));
        assertTrue(batch2.isEmpty());

        assertEquals(
                SearchStaxFullIndexRetryPolicy.MAX_POST_ATTEMPTS,
                payloads.stream().filter(p -> p.contains("/content/one")).count());
        assertEquals(1, payloads.stream().filter(p -> p.contains("/content/two")).count());
        assertEquals(1, getFailedBatchCount(service));
    }

    @Test
    void postBatchWithRetry_reusesSameJsonAcrossAttempts() throws Exception {
        final List<String> payloads = new ArrayList<>();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            private int calls;

            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                payloads.add(requestJson);
                calls++;
                if (calls < 3) {
                    return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(503, "down");
                }
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(200, "");
            }
        };

        final String json = "[{\"id\":\"/content/same\"}]";
        invokePostBatchWithRetry(service, json, false, 5, List.of("/content/same"), json.length());

        assertEquals(3, payloads.size());
        assertTrue(payloads.stream().allMatch(json::equals));
    }

    @Test
    void flushBatch_clearsBatchAfterSuccessfulRetry() throws Exception {
        final SearchStaxIndexBatchBuffer batch = bufferWithId("/content/a");

        final AtomicInteger calls = new AtomicInteger();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                calls.incrementAndGet();
                if (calls.get() == 1) {
                    return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(503, "down");
                }
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(200, "");
            }
        };

        assertTrue(invokeFlushBatch(service, batch, false, "DOCUMENT_COUNT_LIMIT"));

        assertTrue(batch.isEmpty());
        assertEquals(2, calls.get());
        assertEquals(0, countFailureFiles());
    }

    @Test
    void runIndexingTraversalPhases_contentIncludesOnly_noLegacyAssetRootCrawl() {
        final List<String> traversalOrder = new ArrayList<>();
        final TestableExecutionService service =
                new TestableExecutionService(failureDir) {
                    @Override
                    void forEachPagePath(
                            final String includeRoot,
                            final boolean recursive,
                            final Consumer<String> consumer) {
                        traversalOrder.add("page:" + includeRoot);
                    }

                    @Override
                    void forEachAssetPath(
                            final String includeRoot,
                            final boolean recursive,
                            final Consumer<String> consumer) {
                        traversalOrder.add("asset:" + includeRoot);
                    }
                };

        service.runIndexingTraversalPhases(
                new String[] {"/content/wknd/us/en", "/content/wknd/us/en/other"},
                Map.of("/content/wknd/us/en", true, "/content/wknd/us/en/other", true),
                new SearchStaxIndexBatchBuffer());

        assertEquals(2, traversalOrder.size());
        assertEquals("page:/content/wknd/us/en", traversalOrder.get(0));
        assertEquals("page:/content/wknd/us/en/other", traversalOrder.get(1));
        assertTrue(traversalOrder.stream().noneMatch(entry -> entry.startsWith("asset:")));
    }

    @Test
    void runIndexingTraversalPhases_damInclude_invokesAssetTraversalForIncludeRoot() {
        final List<String> traversalOrder = new ArrayList<>();
        final TestableExecutionService service =
                new TestableExecutionService(failureDir) {
                    @Override
                    void forEachPagePath(
                            final String includeRoot,
                            final boolean recursive,
                            final Consumer<String> consumer) {
                        traversalOrder.add("page:" + includeRoot);
                    }

                    @Override
                    void forEachAssetPath(
                            final String includeRoot,
                            final boolean recursive,
                            final Consumer<String> consumer) {
                        traversalOrder.add("asset:" + includeRoot);
                    }
                };

        final String damInclude = "/content/dam/wknd-shared/en/adventures";
        service.runIndexingTraversalPhases(new String[] {damInclude}, Map.of(damInclude, true), new SearchStaxIndexBatchBuffer());

        assertEquals(1, traversalOrder.size());
        assertEquals("asset:" + damInclude, traversalOrder.get(0));
    }

    @Test
    void runIndexingTraversalPhases_usesSameBatchInstanceAcrossPhases() {
        final SearchStaxIndexBatchBuffer batch = new SearchStaxIndexBatchBuffer();
        final AtomicInteger batchIdentityChecks = new AtomicInteger();
        final TestableExecutionService service =
                new TestableExecutionService(failureDir) {
                    @Override
                    void forEachPagePath(
                            final String includeRoot,
                            final boolean recursive,
                            final Consumer<String> consumer) {
                        consumer.accept("/content/wknd/us/en/page-one");
                    }

                    @Override
                    void collectPath(
                            final String path,
                            final boolean page,
                            final SearchStaxIndexBatchBuffer phaseBatch,
                            final Set<String> referencedAssets,
                            final Set<String> processedAssetPaths) {
                        if (phaseBatch == batch) {
                            batchIdentityChecks.incrementAndGet();
                        }
                        if (page && referencedAssets != null) {
                            referencedAssets.add("/content/dam/wknd/ref.jpg");
                        }
                    }
                };

        service.runIndexingTraversalPhases(
                new String[] {"/content/wknd/us/en"}, Map.of("/content/wknd/us/en", true), batch);

        assertTrue(batchIdentityChecks.get() >= 2, "page and referenced-asset phases must share the same batch list");
    }

    @Test
    void collectAssetPathIfNotProcessed_skipsAlreadyProcessedAsset() throws Exception {
        final Set<String> processedAssetPaths = ConcurrentHashMap.newKeySet();
        processedAssetPaths.add("/content/dam/wknd/already.jpg");
        final AtomicInteger collectPathCalls = new AtomicInteger();
        final TestableExecutionService service =
                new TestableExecutionService(failureDir) {
                    @Override
                    void collectPath(
                            final String path,
                            final boolean page,
                            final SearchStaxIndexBatchBuffer batch,
                            final Set<String> referencedAssets,
                            final Set<String> processedAssetPathsArg) {
                        collectPathCalls.incrementAndGet();
                    }
                };

        final Method method =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredMethod(
                        "collectAssetPathIfNotProcessed", String.class, SearchStaxIndexBatchBuffer.class, Set.class);
        method.setAccessible(true);
        final SearchStaxIndexBatchBuffer batch = new SearchStaxIndexBatchBuffer();

        method.invoke(service, "/content/dam/wknd/already.jpg", batch, processedAssetPaths);

        assertEquals(0, collectPathCalls.get());
    }

    @Test
    void collectAssetPathIfNotProcessed_indexesOnceThenSkipsDuplicate() throws Exception {
        final Set<String> processedAssetPaths = ConcurrentHashMap.newKeySet();
        final AtomicInteger collectPathCalls = new AtomicInteger();
        final TestableExecutionService service =
                new TestableExecutionService(failureDir) {
                    @Override
                    void collectPath(
                            final String path,
                            final boolean page,
                            final SearchStaxIndexBatchBuffer batch,
                            final Set<String> referencedAssets,
                            final Set<String> processedAssetPathsArg) {
                        collectPathCalls.incrementAndGet();
                        if (!page && processedAssetPathsArg != null) {
                            processedAssetPathsArg.add(path);
                        }
                    }
                };

        final Method method =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredMethod(
                        "collectAssetPathIfNotProcessed", String.class, SearchStaxIndexBatchBuffer.class, Set.class);
        method.setAccessible(true);
        final SearchStaxIndexBatchBuffer batch = new SearchStaxIndexBatchBuffer();
        final String assetPath = "/content/dam/wknd/hero.jpg";

        method.invoke(service, assetPath, batch, processedAssetPaths);
        method.invoke(service, assetPath, batch, processedAssetPaths);

        assertEquals(1, collectPathCalls.get());
        assertTrue(processedAssetPaths.contains(assetPath));
    }

    @Test
    void postBatchWithRetry_interruptedBackoffAbortsWithoutFailureFile() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final TestableExecutionService service = new TestableExecutionService(failureDir) {
            @Override
            SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                    final String requestJson, final boolean hardCommit, final String contentPath) {
                calls.incrementAndGet();
                return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(503, "down");
            }

            @Override
            void sleepBackoff(final long delayMs) throws IOException {
                Thread.currentThread().interrupt();
                throw new IOException("Full index batch retry interrupted during backoff", new InterruptedException());
            }
        };

        final IOException thrown =
                assertThrows(
                        IOException.class,
                        () -> invokePostBatchWithRetry(service, "[]", false, 9, List.of(), 2));

        assertTrue(thrown.getMessage().contains("interrupted"));
        assertEquals(1, calls.get());
        assertTrue(Thread.interrupted());
        assertEquals(0, countFailureFiles());
    }

    @Test
    void flushBatch_postsPayloadWithinMaxBatchLimit() throws Exception {
        final List<Integer> postedPayloadSizes = new ArrayList<>();
        final TestableExecutionService service =
                new TestableExecutionService(failureDir) {
                    @Override
                    SearchStaxFullIndexExecutionServiceImpl.BatchPostResult postBatchOnce(
                            final String requestJson, final boolean hardCommit, final String contentPath) {
                        postedPayloadSizes.add(requestJson.getBytes(StandardCharsets.UTF_8).length);
                        return new SearchStaxFullIndexExecutionServiceImpl.BatchPostResult(200, "");
                    }
                };

        final SearchStaxIndexBatchBuffer batch = new SearchStaxIndexBatchBuffer();
        final String json =
                OBJECT_MAPPER.writeValueAsString(Map.of("id", "/content/large", "body", "x".repeat(200_000)));
        final int bytes = json.getBytes(StandardCharsets.UTF_8).length;
        while (!batch.wouldExceed(bytes)) {
            batch.add(new SearchStaxIndexBatchEntry(Map.of("id", "/content/x"), "/content/x", true, json, bytes));
        }

        assertTrue(invokeFlushBatch(service, batch, false, "PAYLOAD_SIZE_LIMIT"));
        assertFalse(postedPayloadSizes.isEmpty());
        assertTrue(postedPayloadSizes.get(0) <= SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES);
        assertTrue(batch.isEmpty());
    }

    private static SearchStaxIndexBatchBuffer bufferWithId(final String id) throws Exception {
        final SearchStaxIndexBatchBuffer buffer = new SearchStaxIndexBatchBuffer();
        final Map<String, Object> document = new HashMap<>();
        document.put("id", id);
        final String json = OBJECT_MAPPER.writeValueAsString(document);
        final int bytes = json.getBytes(StandardCharsets.UTF_8).length;
        buffer.add(new SearchStaxIndexBatchEntry(document, id, true, json, bytes));
        return buffer;
    }

    private boolean invokeFlushBatch(
            final TestableExecutionService service,
            final SearchStaxIndexBatchBuffer batch,
            final boolean finalBatch,
            final String flushReasonName)
            throws Exception {
        final Method method =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredMethod(
                        "flushBatch", SearchStaxIndexBatchBuffer.class, boolean.class, flushReasonEnumClass());
        method.setAccessible(true);
        return (Boolean) method.invoke(service, batch, finalBatch, flushReason(flushReasonName));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum> flushReasonEnumClass() throws ClassNotFoundException {
        for (final Class<?> nested : SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredClasses()) {
            if ("BatchFlushReason".equals(nested.getSimpleName()) && nested.isEnum()) {
                return (Class<? extends Enum>) nested;
            }
        }
        throw new ClassNotFoundException("BatchFlushReason");
    }

    @SuppressWarnings("unchecked")
    private static Object flushReason(final String name) throws Exception {
        return Enum.valueOf(flushReasonEnumClass(), name);
    }

    private int getFailedBatchCount(final TestableExecutionService service) throws Exception {
        final java.lang.reflect.Field field =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredField("failedBatchCount");
        field.setAccessible(true);
        return field.getInt(service);
    }

    private SearchStaxFullIndexExecutionServiceImpl.BatchPostResult invokePostBatchWithRetry(
            final TestableExecutionService service,
            final String json,
            final boolean hardCommit,
            final int batchNum,
            final List<String> paths,
            final int payloadBytes)
            throws Exception {
        final Method method =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredMethod(
                        "postBatchWithRetry",
                        String.class,
                        boolean.class,
                        int.class,
                        List.class,
                        int.class);
        method.setAccessible(true);
        try {
            return (SearchStaxFullIndexExecutionServiceImpl.BatchPostResult)
                    method.invoke(service, json, hardCommit, batchNum, paths, payloadBytes);
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }

    @Test
    void sendConsolidatedFailureEmailIfNeeded_sendsOnceWhenMultipleBatchFailures() throws Exception {
        final CountingEmailService emailService = new CountingEmailService();
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[]{"test@example.com"});
        final TestableExecutionService service =
                new TestableExecutionService(failureDir, emailService, emailConfigService);
        final Instant runStart = Instant.parse("2026-05-20T12:00:00Z");
        setField(service, "startedAt", runStart.toEpochMilli());
        setField(service, "failedBatchCount", 2);
        setField(service, "state", FullIndexProgress.State.PARTIAL_FAILURE);
        setField(service, "progressMessage", "Full index completed with 2 failed batch(es)");

        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(failureDir);
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-2",
                        List.of("/content/a"),
                        429,
                        "Too Many Requests",
                        100,
                        Instant.parse("2026-05-20T12:10:00Z"),
                        6));
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-4",
                        List.of("/content/b"),
                        503,
                        "Service Unavailable",
                        200,
                        Instant.parse("2026-05-20T12:20:00Z"),
                        6));

        invokeSendConsolidatedFailureEmailIfNeeded(service);

        assertEquals(1, emailService.getSendCount());
        assertEquals("SearchStax Full Index Completed With Failures", emailService.getLastSubject());
        assertTrue(emailService.getLastBody().contains("batch-2"));
        assertTrue(emailService.getLastBody().contains("batch-4"));
    }

    @Test
    void sendConsolidatedFailureEmailIfNeeded_doesNotSendWhenNoBatchFailures() throws Exception {
        final CountingEmailService emailService = new CountingEmailService();
        final TestableExecutionService service =
                new TestableExecutionService(failureDir, emailService, emailConfigService);
        setField(service, "failedBatchCount", 0);

        invokeSendConsolidatedFailureEmailIfNeeded(service);

        assertEquals(0, emailService.getSendCount());
    }

    @Test
    void sendConsolidatedFailureEmailIfNeeded_sendsForPathOnlyFailures() throws Exception {
        final CountingEmailService emailService = new CountingEmailService();
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[]{"test@example.com"});
        final TestableExecutionService service =
                new TestableExecutionService(failureDir, emailService, emailConfigService);
        final Instant runStart = Instant.parse("2026-05-20T12:00:00Z");
        setField(service, "startedAt", runStart.toEpochMilli());
        setField(service, "failedBatchCount", 0);
        setField(service, "failureCount", 1L);

        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(failureDir);
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "path-payload-limit-_content_wknd_large",
                        List.of("/content/wknd/large-payload-lorem-test-new"),
                        413,
                        "Single document payload 15658975 bytes exceeds max batch payload 10485760 bytes",
                        15_658_975,
                        Instant.parse("2026-05-20T12:05:00Z"),
                        0));

        invokeSendConsolidatedFailureEmailIfNeeded(service);

        assertEquals(1, emailService.getSendCount());
        assertTrue(emailService.getLastBody().contains("Path (no retry)"));
        assertTrue(emailService.getLastBody().contains("/content/wknd/large-payload-lorem-test-new"));
        assertFalse(emailService.getLastBody().contains("Retry Attempts"));
    }

    @Test
    void recordPathFailure_persistsImmediatelyWithoutRetry() throws Exception {
        final TestableExecutionService service = new TestableExecutionService(failureDir);
        final String path = "/content/wknd/language-masters/en/large-payload-lorem-test-new";

        invokeRecordPathFailure(
                service,
                path,
                "payload-limit",
                "Single document payload 15658975 bytes exceeds max batch payload 10485760 bytes",
                15_658_975,
                413);

        assertEquals(1, countFailureFiles());
        final List<SearchStaxFullIndexFailureStore.FailureRecord> records =
                new SearchStaxFullIndexFailureStore(failureDir)
                        .listFailuresSince(Instant.EPOCH);
        assertEquals(1, records.size());
        assertEquals(0, records.get(0).getRetryAttempts());
        assertEquals(413, records.get(0).getStatusCode());
        assertEquals(path, records.get(0).getPaths().get(0));
        assertTrue(records.get(0).getBatchId().startsWith("path-payload-limit-"));
    }

    @Test
    void collectPath_oversizedDocument_recordsPathFailureWithoutBatching() throws Exception {
        final String path = "/content/wknd/language-masters/en/large-payload-lorem-test-new";
        final String hugeBody = "x".repeat(SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES);
        final Map<String, Object> document = new HashMap<>();
        document.put("id", path);
        document.put("body", hugeBody);

        final TestableExecutionService service = new TestableExecutionService(failureDir);
        setField(service, "resolverUtil", resolverUtil);
        setField(service, "documentBuilder", documentBuilder);
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(documentBuilder.buildPageIfPublished(resourceResolver, path)).thenReturn(document);

        final Method collectPath =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredMethod(
                        "collectPath",
                        String.class,
                        boolean.class,
                        SearchStaxIndexBatchBuffer.class,
                        Set.class,
                        Set.class);
        collectPath.setAccessible(true);
        collectPath.invoke(service, path, true, new SearchStaxIndexBatchBuffer(), null, null);

        assertEquals(1, countFailureFiles());
        assertEquals(1L, getField(service, "failureCount"));
    }

    private void invokeSendConsolidatedFailureEmailIfNeeded(final TestableExecutionService service)
            throws Exception {
        final Method method =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredMethod(
                        "sendConsolidatedFailureEmailIfNeeded");
        method.setAccessible(true);
        method.invoke(service);
    }

    private static void invokeRecordPathFailure(
            final TestableExecutionService service,
            final String path,
            final String failureKind,
            final String errorMessage,
            final int payloadBytes,
            final int statusCode)
            throws Exception {
        final Method method =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredMethod(
                        "recordPathFailure",
                        String.class,
                        String.class,
                        String.class,
                        int.class,
                        int.class);
        method.setAccessible(true);
        method.invoke(service, path, failureKind, errorMessage, payloadBytes, statusCode);
    }

    private static Object getField(final Object target, final String name) throws Exception {
        final java.lang.reflect.Field field =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final java.lang.reflect.Field field =
                SearchStaxFullIndexExecutionServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private int countFailureFiles() throws IOException {
        if (!Files.exists(failureDir)) {
            return 0;
        }
        try (var files = Files.list(failureDir)) {
            return (int) files.count();
        }
    }

    private static class TestableExecutionService extends SearchStaxFullIndexExecutionServiceImpl {

        TestableExecutionService(final Path baseDir) {
            this(baseDir, new CountingEmailService(), null);
        }

        TestableExecutionService(
                final Path baseDir,
                final EmailService emailService,
                final EmailConfigService emailConfigService) {
            super(new SearchStaxFullIndexFailureStore(baseDir), emailService, emailConfigService);
            injectDefaultRuntimeConfig(this);
        }

        private static void injectDefaultRuntimeConfig(final SearchStaxFullIndexExecutionServiceImpl target) {
            try {
                final SearchStaxFullIndexRuntimeConfigService defaults =
                        new SearchStaxFullIndexRuntimeConfigService() {
                            @Override
                            public int getBatchSize() {
                                return 100;
                            }

                            @Override
                            public int getQueryPageSize() {
                                return 500;
                            }

                            @Override
                            public TraversalMode getTraversalMode() {
                                return TraversalMode.JCR_SQL2;
                            }

                            @Override
                            public long getCommitWithinMs() {
                                return 120_000L;
                            }

                            @Override
                            public int getHardCommitEveryNBatches() {
                                return 10;
                            }

                            @Override
                            public boolean isHardCommitOnSuccess() {
                                return true;
                            }

                            @Override
                            public long getBatchThrottleMs() {
                                return 300L;
                            }

                            @Override
                            public int getResolverRefreshEveryNBatches() {
                                return 20;
                            }
                        };
                setField(target, "runtimeConfigService", defaults);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final class CountingEmailService implements EmailService {

        private int sendCount;
        private String lastSubject;
        private String lastBody;

        @Override
        public boolean sendEmail(final EmailRequest request) {
            sendCount++;
            lastSubject = request.getSubject();
            lastBody = request.getBody();
            return true;
        }

        @Override
        public String sendEmailOrError(final EmailRequest request) {
            return sendEmail(request) ? null : "Unable to send email.";
        }

        @Override
        public String sendEmailOrError(final EmailRequest request, final EmailConfig config) {
            return sendEmailOrError(request);
        }

        int getSendCount() {
            return sendCount;
        }

        String getLastSubject() {
            return lastSubject;
        }

        String getLastBody() {
            return lastBody;
        }
    }
}
