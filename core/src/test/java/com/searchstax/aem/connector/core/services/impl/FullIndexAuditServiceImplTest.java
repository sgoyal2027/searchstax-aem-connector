package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexAuditService;
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
class FullIndexAuditServiceImplTest {

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource auditRoot;

    @InjectMocks
    private FullIndexAuditServiceImpl fullIndexAuditService;

    @Test
    void listEventsForReport_filtersByStatus() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(FullIndexAuditServiceImpl.AUDIT_ROOT)).thenReturn(auditRoot);

        final List<Resource> auditEvents = List.of(
                auditEvent(FullIndexAuditService.STATUS_SUCCESS, "/content/a"),
                auditEvent("FAILURE", "/content/b"));
        when(auditRoot.getChildren()).thenReturn(auditEvents);

        final List<Map<String, Object>> successes =
                fullIndexAuditService.listEventsForReport(FullIndexAuditService.STATUS_SUCCESS, 50, 24);

        assertEquals(1, successes.size());
        assertEquals("/content/a", successes.get(0).get("path"));
        assertEquals(FullIndexAuditService.STATUS_SUCCESS, successes.get(0).get("status"));
    }

    @Test
    void listEventsForReport_returnsAllWhenStatusFilterIsAll() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(FullIndexAuditServiceImpl.AUDIT_ROOT)).thenReturn(auditRoot);
        final Resource first = auditEvent(FullIndexAuditService.STATUS_SUCCESS, "/content/a");
        final Resource second = auditEvent(FullIndexAuditService.STATUS_SUCCESS, "/content/b");
        when(auditRoot.getChildren()).thenReturn(List.of(first, second));

        final List<Map<String, Object>> events = fullIndexAuditService.listEventsForReport("ALL", 50, 24);

        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(event -> FullIndexAuditService.STATUS_SUCCESS.equals(event.get("status"))));
    }

    private static Resource auditEvent(final String status, final String path) {
        final Resource resource = mock(Resource.class);
        final ValueMap valueMap = mock(ValueMap.class);
        final Calendar timestamp = Calendar.getInstance();

        lenient().when(resource.getValueMap()).thenReturn(valueMap);
        lenient().when(valueMap.get("timestamp", Calendar.class)).thenReturn(timestamp);
        lenient().when(valueMap.get("status", "")).thenReturn(status);
        lenient().when(valueMap.get("path", "")).thenReturn(path);
        lenient().when(valueMap.get("action", FullIndexAuditService.ACTION_FULL_REINDEX))
                .thenReturn(FullIndexAuditService.ACTION_FULL_REINDEX);
        lenient().when(valueMap.get("message", "")).thenReturn("Indexed in batch 1");
        lenient().when(valueMap.get("batchId", "")).thenReturn("batch-1");
        lenient().when(valueMap.get("eventKind", "BATCH")).thenReturn("BATCH");
        return resource;
    }
}
