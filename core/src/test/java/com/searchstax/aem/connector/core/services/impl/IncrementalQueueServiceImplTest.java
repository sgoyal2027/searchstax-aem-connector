package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class IncrementalQueueServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource pendingRoot;

    @Mock
    private Resource queueItemOne;

    @Mock
    private Resource queueItemTwo;

    @InjectMocks
    private IncrementalQueueServiceImpl queueService;

    @BeforeEach
    void bindResolver() throws Exception {
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
    void clearPendingQueue_removesAllPendingItems() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(IncrementalQueueServiceImpl.PENDING_QUEUE_PATH)).thenReturn(pendingRoot);
        when(pendingRoot.getChildren()).thenReturn(List.of(queueItemOne, queueItemTwo));

        final int removed = queueService.clearPendingQueue();

        assertEquals(2, removed);
        verify(resourceResolver).delete(queueItemOne);
        verify(resourceResolver).delete(queueItemTwo);
        verify(resourceResolver).commit();
    }

    @Test
    void clearPendingQueue_returnsZeroWhenQueueMissing() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(IncrementalQueueServiceImpl.PENDING_QUEUE_PATH)).thenReturn(null);

        assertEquals(0, queueService.clearPendingQueue());
    }

    @Test
    void addRequest_getBatchAndRemoveProcessed_manageQueueItems() {
        queueService.addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, "corr-1");
        queueService.addRequest("/content/wknd/en/about", ReplicationActionType.ACTIVATE, "corr-2");

        assertEquals(2, queueService.size());

        final List<IndexRequest> batch = queueService.getBatch(1);
        assertEquals(1, batch.size());

        queueService.removeProcessed(batch);
        assertEquals(1, queueService.size());
    }

    @Test
    void addRequest_updatesExistingPathInsteadOfDuplicating() {
        queueService.addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, "corr-1");
        queueService.addRequest("/content/wknd/en/page", ReplicationActionType.DELETE, "corr-2");

        assertEquals(1, queueService.size());
        final IndexRequest request = queueService.getBatch(10).get(0);
        assertEquals(ReplicationActionType.DELETE, request.getActionType());
        assertEquals("corr-2", request.getCorrelationId());
    }

    @Test
    void updateRequest_incrementsRetryCount() {
        queueService.addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, "corr-1");
        final IndexRequest request = queueService.getBatch(10).get(0);
        request.setRetryCount(2);

        queueService.updateRequest(request);

        final IndexRequest updated = queueService.getBatch(10).get(0);
        assertEquals(2, updated.getRetryCount());
    }

    @Test
    void incrementRetryCount_updatesPersistedValue() {
        queueService.addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, "corr-1");
        final IndexRequest request = queueService.getBatch(10).get(0);

        queueService.incrementRetryCount(request);

        final IndexRequest updated = queueService.getBatch(10).get(0);
        assertEquals(1, updated.getRetryCount());
    }

    @Test
    void getBatch_returnsEmptyListForNonPositiveBatchSize() {
        queueService.addRequest("/content/wknd/en/page", ReplicationActionType.ACTIVATE, "corr-1");

        assertTrue(queueService.getBatch(0).isEmpty());
    }
}
