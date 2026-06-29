package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class IncrementalQueueServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @InjectMocks
    private IncrementalQueueServiceImpl service;

    @Mock
    private ResolverUtil resolverUtil;

    @BeforeEach
    void setup() throws LoginException {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void nodeNameForPath_sanitizesSpecialCharacters() {
        final String nodeName =
                IncrementalQueueServiceImpl.nodeNameForPath("/content/wknd/en/my page");

        assertFalse(nodeName.contains(" "));
        assertFalse(nodeName.contains("/"));
    }

    @Test
    void nodeNameForPath_truncatesVeryLongPaths() {
        final String longPath = "/content/" + "a".repeat(250);
        final String nodeName = IncrementalQueueServiceImpl.nodeNameForPath(longPath);

        assertTrue(nodeName.length() <= 200);
    }

    @Test
    void addRequest_createsPendingNodeWithCorrelationId() {
        service.addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, "corr-1");

        assertEquals(1, service.size());
        final IndexRequest request = service.getBatch(10).get(0);
        assertEquals("/content/wknd/en/page", request.getPath());
        assertEquals(ReplicationActionType.ACTIVATE, request.getActionType());
        assertEquals("corr-1", request.getCorrelationId());
    }

    @Test
    void addRequest_updatesExistingPathInsteadOfDuplicating() {
        service.addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, null);
        service.addRequest("/content/wknd/en/page", ReplicationActionType.DELETE, "corr-2");

        assertEquals(1, service.size());
        final IndexRequest request = service.getBatch(1).get(0);
        assertEquals(ReplicationActionType.DELETE, request.getActionType());
        assertEquals("corr-2", request.getCorrelationId());
    }

    @Test
    void addRequest_skipsInvalidInput() {
        service.addRequest(null, ReplicationActionType.ACTIVATE, null);
        service.addRequest("  ", ReplicationActionType.ACTIVATE, null);
        service.addRequest("/content/page", null, null);

        assertEquals(0, service.size());
    }

    @Test
    void getBatch_returnsEmptyWhenQueueMissing() {
        assertTrue(service.getBatch(10).isEmpty());
    }

    @Test
    void getBatch_respectsBatchSizeAndEventOrder() throws Exception {
        service.addRequest("/content/a", ReplicationActionType.ACTIVATE, null);
        Thread.sleep(5);
        service.addRequest("/content/b", ReplicationActionType.ACTIVATE, null);

        final List<IndexRequest> batch = service.getBatch(1);

        assertEquals(1, batch.size());
        assertEquals("/content/a", batch.get(0).getPath());
    }

    @Test
    void getBatch_returnsEmptyForNonPositiveBatchSize() {
        service.addRequest("/content/a", ReplicationActionType.ACTIVATE, null);

        assertTrue(service.getBatch(0).isEmpty());
        assertTrue(service.getBatch(-1).isEmpty());
    }

    @Test
    void getBatch_skipsInvalidActionTypeNodes() {
        context.create()
                .resource(
                        IncrementalQueueServiceImpl.PENDING_QUEUE_PATH + "/invalid",
                        "path",
                        "/content/bad",
                        "actionType",
                        "NOT_A_REAL_ACTION",
                        "eventTime",
                        1L);

        assertTrue(service.getBatch(10).isEmpty());
    }

    @Test
    void removeProcessed_deletesMatchingNodes() {
        service.addRequest("/content/a", ReplicationActionType.ACTIVATE, null);
        service.addRequest("/content/b", ReplicationActionType.DEACTIVATE, null);

        final List<IndexRequest> toRemove = new ArrayList<>(service.getBatch(10));
        service.removeProcessed(toRemove);

        assertEquals(0, service.size());
    }

    @Test
    void removeProcessed_ignoresNullAndEmptyLists() {
        service.addRequest("/content/a", ReplicationActionType.ACTIVATE, null);

        service.removeProcessed(null);
        service.removeProcessed(List.of());

        assertEquals(1, service.size());
    }

    @Test
    void updateRequest_persistsRetryCountAndActionType() {
        service.addRequest("/content/a", ReplicationActionType.ACTIVATE, null);

        final IndexRequest request = service.getBatch(1).get(0);
        request.setRetryCount(3);
        request.setActionType(ReplicationActionType.DELETE);
        service.updateRequest(request);

        final IndexRequest updated = service.getBatch(1).get(0);
        assertEquals(3, updated.getRetryCount());
        assertEquals(ReplicationActionType.DELETE, updated.getActionType());
    }

    @Test
    void incrementRetryCount_incrementsAndPersists() {
        service.addRequest("/content/a", ReplicationActionType.ACTIVATE, null);

        final IndexRequest request = service.getBatch(1).get(0);
        service.incrementRetryCount(request);

        assertEquals(1, service.getBatch(1).get(0).getRetryCount());
    }

    @Test
    void updateRequest_noOpForNullRequestOrMissingNode() {
        service.updateRequest(null);

        final IndexRequest missing = new IndexRequest();
        missing.setPath("/content/missing");
        missing.setRetryCount(1);
        service.updateRequest(missing);
    }
}
