package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class IndexingAuditServiceImplTest {

    private static final String FAILED_ROOT = "/var/searchstaxconnector/incremental-index/failed";

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private IncrementalQueueService incrementalQueueService;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource auditRoot;

    @InjectMocks
    private IndexingAuditServiceImpl indexingAuditService;

    @BeforeEach
    void bindResolver() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void listEvents_excludesQueuedAndFiltersByActionAndStatus() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(IndexingAuditServiceImpl.AUDIT_ROOT)).thenReturn(auditRoot);

        final List<Resource> auditEvents = List.of(
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_QUEUED, "Added to incremental queue"),
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_SUCCESS, "Indexed"),
                auditEvent("DEACTIVATE", IndexingAuditService.STATUS_SUCCESS, "Deactivated"),
                auditEvent("DELETE", IndexingAuditService.STATUS_FAILURE, "DELETE_PERMANENT_FAILURE"),
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_SKIPPED, "Unsupported or missing document"));
        when(auditRoot.getChildren()).thenReturn(auditEvents);

        final List<Map<String, Object>> allVisible = indexingAuditService.listEvents("ALL", "ALL", true, 50);
        assertEquals(4, allVisible.size());
        assertTrue(allVisible.stream().noneMatch(row -> IndexingAuditService.STATUS_QUEUED.equals(row.get("status"))));

        final List<Map<String, Object>> deactivateOnly =
                indexingAuditService.listEvents("ALL", "DEACTIVATE", true, 50);
        assertEquals(1, deactivateOnly.size());
        assertEquals("DEACTIVATE", deactivateOnly.get(0).get("action"));
        assertEquals("Deactivated", deactivateOnly.get(0).get("message"));

        final List<Map<String, Object>> failuresOnly =
                indexingAuditService.listEvents("FAILURE", "ALL", true, 50);
        assertEquals(1, failuresOnly.size());
        assertEquals("DELETE", failuresOnly.get(0).get("action"));
    }

    @Test
    void listEvents_deduplicatesIdenticalRows() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(IndexingAuditServiceImpl.AUDIT_ROOT)).thenReturn(auditRoot);

        final List<Resource> auditEvents = List.of(
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_FAILURE, "PERMANENT_FAILURE"),
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_FAILURE, "PERMANENT_FAILURE"));
        when(auditRoot.getChildren()).thenReturn(auditEvents);

        final List<Map<String, Object>> events = indexingAuditService.listEvents("ALL", "ALL", true, 50);
        assertEquals(1, events.size());
    }

    @Test
    void listEventsPaged_returnsRequestedSliceAndTotalCount() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(IndexingAuditServiceImpl.AUDIT_ROOT)).thenReturn(auditRoot);

        final List<Resource> auditEvents = List.of(
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_SUCCESS, "One"),
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_SUCCESS, "Two"),
                auditEvent("ACTIVATE", IndexingAuditService.STATUS_SUCCESS, "Three"));
        when(auditRoot.getChildren()).thenReturn(auditEvents);

        final var page = indexingAuditService.listEventsPaged("ALL", "ALL", true, 0, 2);
        assertEquals(3, page.getTotalCount());
        assertEquals(2, page.getEvents().size());

        final var lastPage = indexingAuditService.listEventsPaged("ALL", "ALL", true, 2, 2);
        assertEquals(3, lastPage.getTotalCount());
        assertEquals(1, lastPage.getEvents().size());
    }

    @Test
    void recordEvent_persistsAuditNode() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());

        indexingAuditService.recordEvent(
                "/content/wknd/en/page",
                "ACTIVATE",
                IndexingAuditService.STATUS_SUCCESS,
                "Indexed",
                "corr-1",
                250L,
                "batch-1");

        final Resource auditRoot = context.resourceResolver().getResource(IndexingAuditServiceImpl.AUDIT_ROOT);
        assertEquals(1, countChildren(auditRoot));
        final ValueMap valueMap = auditRoot.getChildren().iterator().next().getValueMap();
        assertEquals("/content/wknd/en/page", valueMap.get("path", String.class));
        assertEquals("corr-1", valueMap.get("correlationId", String.class));
        assertEquals(250L, valueMap.get("durationMs", Long.class));
    }

    @Test
    void reprocessFailedPath_removesFailedRecordAndRequeues() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        context.create().resource(FAILED_ROOT, "jcr:primaryType", "sling:Folder");
        context.create().resource(
                FAILED_ROOT + "/failed-1",
                "path", "/content/wknd/en/page",
                "actionType", "DELETE");

        indexingAuditService.reprocessFailedPath("/content/wknd/en/page");

        assertEquals(0, countChildren(context.resourceResolver().getResource(FAILED_ROOT)));
        verify(incrementalQueueService).addRequest("/content/wknd/en/page", ReplicationActionType.DELETE);
    }

    @Test
    void clearAllEvents_removesIncrementalAuditNodes() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        context.create().resource(IndexingAuditServiceImpl.AUDIT_ROOT, "jcr:primaryType", "sling:Folder");
        context.create().resource(IndexingAuditServiceImpl.AUDIT_ROOT + "/event-1", "path", "/content/a");

        assertEquals(1, indexingAuditService.clearAllEvents());
        assertEquals(0, countChildren(context.resourceResolver().getResource(IndexingAuditServiceImpl.AUDIT_ROOT)));
    }

    private static int countChildren(final Resource resource) {
        int count = 0;
        if (resource != null) {
            for (final Resource ignored : resource.getChildren()) {
                count++;
            }
        }
        return count;
    }

    private static Resource auditEvent(final String action, final String status, final String message) {
        final Resource resource = mock(Resource.class);
        final ValueMap valueMap = mock(ValueMap.class);
        final Calendar timestamp = Calendar.getInstance();

        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("timestamp", Calendar.class)).thenReturn(timestamp);
        lenient().when(valueMap.get("status", "")).thenReturn(status);
        lenient().when(valueMap.get("action", "")).thenReturn(action);
        lenient().when(valueMap.get("path", "")).thenReturn("/content/wknd/en/page");
        lenient().when(valueMap.get("durationMs", 0L)).thenReturn(120L);
        lenient().when(valueMap.get("message", "")).thenReturn(message);
        return resource;
    }
}
