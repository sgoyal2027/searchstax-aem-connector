package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingAuditServiceImplTest {

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource auditRoot;

    @InjectMocks
    private IndexingAuditServiceImpl indexingAuditService;

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
