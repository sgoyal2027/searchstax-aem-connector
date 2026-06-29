package com.searchstax.aem.connector.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testutil.OsgiFieldInjector;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchStaxFullIndexFailureStoreTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AemContext aemContext = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @TempDir
    Path tempDir;

    @BeforeEach
    void bindJcrResolver() throws Exception {
        aemContext.create().resource("/var", "jcr:primaryType", "sling:Folder");
        when(resolverUtil.getServiceResolver()).thenReturn(aemContext.resourceResolver());
    }

    @Test
    void recordFailure_writesAllRequiredFields() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        final Instant timestamp = Instant.parse("2026-05-20T12:00:00Z");

        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-7",
                        List.of("/content/a", "/content/b"),
                        503,
                        "Service Unavailable",
                        4096,
                        timestamp,
                        6));

        try (var files = Files.list(tempDir)) {
            final Path failureFile = files.findFirst().orElseThrow();
            final JsonNode json = OBJECT_MAPPER.readTree(failureFile.toFile());

            assertEquals("batch-7", json.get("batchId").asText());
            assertEquals(2, json.get("paths").size());
            assertEquals("/content/a", json.get("paths").get(0).asText());
            assertEquals(503, json.get("statusCode").asInt());
            assertEquals("Service Unavailable", json.get("errorMessage").asText());
            assertEquals(4096, json.get("payloadBytes").asInt());
            assertEquals("2026-05-20T12:00:00Z", json.get("timestamp").asText());
            assertEquals(6, json.get("retryAttempts").asInt());
        }
    }

    @Test
    void jcrRunsPath_isRepositoryLocation() {
        assertEquals("/var/searchstaxconnector/fullindex/failure", SearchStaxFullIndexFailureStore.JCR_RUNS_PATH);
    }

    @Test
    void truncateErrorMessage_capsAt8Kb() {
        final String truncated =
                SearchStaxFullIndexFailureStore.truncateErrorMessage("x".repeat(10_000));
        assertEquals(8 * 1024, truncated.length());
    }

    @Test
    void truncateErrorMessage_handlesNullAndShortValues() {
        assertEquals("", SearchStaxFullIndexFailureStore.truncateErrorMessage(null));
        assertEquals("short", SearchStaxFullIndexFailureStore.truncateErrorMessage("short"));
    }

    @Test
    void listFailuresSince_returnsEmptyWhenDirectoryMissing() throws Exception {
        final Path missing = tempDir.resolve("does-not-exist");
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(missing);

        assertTrue(store.listFailuresSince(Instant.EPOCH).isEmpty());
    }

    @Test
    void clearAllFailures_returnsZeroForEmptyDirectory() throws Exception {
        Files.createDirectories(tempDir);
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);

        assertEquals(0, store.clearAllFailures());
    }

    @Test
    void listFailureEventsForReport_includesAllStatusesWhenFilterIsAll() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-all",
                        List.of("/content/a"),
                        503,
                        "failed",
                        100,
                        Instant.now(),
                        1));

        assertEquals(1, store.listFailureEventsForReport("ALL", 10, 24).size());
    }

    @Test
    void recordFailure_jcr_persistsAndListsRecords() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore();
        OsgiFieldInjector.inject(store, "resolverUtil", resolverUtil);
        final Instant timestamp = Instant.parse("2026-06-29T10:00:00Z");

        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-jcr-1",
                        List.of("/content/wknd/en/page"),
                        503,
                        "Service Unavailable",
                        512,
                        timestamp,
                        3));

        final List<SearchStaxFullIndexFailureStore.FailureRecord> failures =
                store.listFailuresSince(Instant.parse("2026-06-29T09:00:00Z"));

        assertEquals(1, failures.size());
        assertEquals("batch-jcr-1", failures.get(0).getBatchId());
        assertEquals(503, failures.get(0).getStatusCode());
        assertEquals("/content/wknd/en/page", failures.get(0).getPaths().get(0));

        final Resource runsRoot = aemContext.resourceResolver().getResource(SearchStaxFullIndexFailureStore.JCR_RUNS_PATH);
        assertTrue(runsRoot != null && runsRoot.hasChildren());
    }

    @Test
    void clearAllFailures_jcr_removesPersistedRecords() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore();
        OsgiFieldInjector.inject(store, "resolverUtil", resolverUtil);
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-jcr-clear",
                        List.of("/content/a"),
                        500,
                        "error",
                        100,
                        Instant.now(),
                        1));

        assertEquals(1, store.clearAllFailures());
        assertTrue(store.listFailuresSince(Instant.EPOCH).isEmpty());
    }

    @Test
    void recordFailure_createsParentDirectories() throws Exception {
        final Path nested = tempDir.resolve("failures/nested");
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(nested);

        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-1",
                        List.of(),
                        429,
                        "Too Many Requests",
                        2,
                        Instant.now(),
                        6));

        assertTrue(Files.isDirectory(nested));
        try (var files = Files.list(nested)) {
            assertTrue(files.findAny().isPresent());
        }
    }

    @Test
    void listFailuresSince_returnsRecordsOnOrAfterCutoff() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        final Instant runStart = Instant.parse("2026-05-20T12:00:00Z");
        final Instant beforeRun = Instant.parse("2026-05-20T11:00:00Z");
        final Instant duringRun = Instant.parse("2026-05-20T12:30:00Z");

        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-1",
                        List.of("/content/old"),
                        503,
                        "old failure",
                        100,
                        beforeRun,
                        6));
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-2",
                        List.of("/content/new-a"),
                        429,
                        "new failure a",
                        200,
                        duringRun,
                        6));
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-4",
                        List.of("/content/new-b"),
                        500,
                        "new failure b",
                        300,
                        Instant.parse("2026-05-20T13:00:00Z"),
                        3));

        final List<SearchStaxFullIndexFailureStore.FailureRecord> failures =
                store.listFailuresSince(runStart);

        assertEquals(2, failures.size());
        assertTrue(failures.stream().anyMatch(r -> "batch-2".equals(r.getBatchId())));
        assertTrue(failures.stream().anyMatch(r -> "batch-4".equals(r.getBatchId())));
        assertFalse(failures.stream().anyMatch(r -> "batch-1".equals(r.getBatchId())));
    }

    @Test
    void listFailureEventsForReport_flattensPathsAndFiltersByRetention() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        final Instant recent = Instant.now().minus(1, ChronoUnit.HOURS);
        final Instant old = Instant.now().minus(48, ChronoUnit.HOURS);

        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-10",
                        List.of("/content/a", "/content/b"),
                        503,
                        "Service Unavailable",
                        4096,
                        recent,
                        2));
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "path-document-limit-/content/c",
                        List.of("/content/c"),
                        413,
                        "Payload too large",
                        2048,
                        recent,
                        0));
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-old",
                        List.of("/content/old"),
                        500,
                        "old failure",
                        100,
                        old,
                        1));

        final List<Map<String, Object>> events = store.listFailureEventsForReport("FAILURE", 50, 24);

        assertEquals(3, events.size());
        assertTrue(events.stream().anyMatch(e -> "/content/a".equals(e.get("path"))));
        assertTrue(events.stream().anyMatch(e -> "/content/b".equals(e.get("path"))));
        assertTrue(events.stream().anyMatch(e -> "PATH".equals(e.get("failureKind"))));
        assertTrue(events.stream().anyMatch(e -> "BATCH".equals(e.get("failureKind"))));
        assertFalse(events.stream().anyMatch(e -> "/content/old".equals(e.get("path"))));
    }

    @Test
    void listFailureEventsForReport_returnsEmptyForSuccessStatusFilter() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-1",
                        List.of("/content/a"),
                        503,
                        "Service Unavailable",
                        100,
                        Instant.now(),
                        1));

        assertTrue(store.listFailureEventsForReport("SUCCESS", 50, 24).isEmpty());
    }

    @Test
    void clearAllFailures_removesFilesystemRecords() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-1",
                        List.of("/content/a"),
                        503,
                        "Service Unavailable",
                        100,
                        Instant.now(),
                        1));
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-2",
                        List.of("/content/b"),
                        500,
                        "Server error",
                        200,
                        Instant.now(),
                        2));

        assertEquals(2, store.clearAllFailures());
        assertTrue(store.listFailuresSince(Instant.EPOCH).isEmpty());
    }

    @Test
    void listFailureEventsForReport_limitsResultsToMaxResults() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        for (int i = 0; i < 5; i++) {
            store.recordFailure(
                    new SearchStaxFullIndexFailureStore.FailureRecord(
                            "batch-" + i,
                            List.of("/content/page-" + i),
                            503,
                            "failed",
                            100,
                            Instant.now(),
                            1));
        }

        assertEquals(2, store.listFailureEventsForReport("FAILURE", 2, 24).size());
    }

    @Test
    void listFailureEventsForReport_usesBatchIdWhenPathsEmpty() throws Exception {
        final SearchStaxFullIndexFailureStore store = new SearchStaxFullIndexFailureStore(tempDir);
        store.recordFailure(
                new SearchStaxFullIndexFailureStore.FailureRecord(
                        "batch-empty-paths",
                        List.of(),
                        503,
                        "batch failed",
                        100,
                        Instant.now(),
                        3));

        final List<Map<String, Object>> events = store.listFailureEventsForReport("FAILURE", 10, 24);

        assertEquals(1, events.size());
        assertEquals("batch-empty-paths", events.get(0).get("path"));
        assertEquals("BATCH", events.get(0).get("failureKind"));
    }
}
