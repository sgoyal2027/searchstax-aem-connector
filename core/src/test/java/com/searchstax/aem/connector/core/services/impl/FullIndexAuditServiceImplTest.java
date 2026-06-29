package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexAuditService;
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
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class FullIndexAuditServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @InjectMocks
    private FullIndexAuditServiceImpl fullIndexAuditService;

    @BeforeEach
    void bindResolver() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void listEventsForReport_filtersByStatus() throws Exception {
        context.create().resource(FullIndexAuditServiceImpl.AUDIT_ROOT, "jcr:primaryType", "sling:Folder");
        context.create().resource(
                FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-success",
                "path", "/content/a",
                "status", FullIndexAuditService.STATUS_SUCCESS,
                "action", FullIndexAuditService.ACTION_FULL_REINDEX,
                "message", "Indexed in batch 1",
                "batchId", "batch-1",
                "eventKind", "BATCH",
                "timestamp", Calendar.getInstance());
        context.create().resource(
                FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-failure",
                "path", "/content/b",
                "status", "FAILURE",
                "action", FullIndexAuditService.ACTION_FULL_REINDEX,
                "message", "Failed",
                "batchId", "batch-2",
                "eventKind", "BATCH",
                "timestamp", Calendar.getInstance());

        final List<Map<String, Object>> successes =
                fullIndexAuditService.listEventsForReport(FullIndexAuditService.STATUS_SUCCESS, 50, 24);

        assertEquals(1, successes.size());
        assertEquals("/content/a", successes.get(0).get("path"));
        assertEquals(FullIndexAuditService.STATUS_SUCCESS, successes.get(0).get("status"));
    }

    @Test
    void recordSuccessBatch_persistsAuditNodes() {
        fullIndexAuditService.recordSuccessBatch(List.of("/content/wknd/en/page", "/content/wknd/en/about"), 2, 500L);

        final Resource auditRoot = context.resourceResolver().getResource(FullIndexAuditServiceImpl.AUDIT_ROOT);
        int childCount = 0;
        for (final Resource ignored : auditRoot.getChildren()) {
            childCount++;
        }
        assertEquals(2, childCount);
    }

    @Test
    void clearAllEvents_removesAuditChildren() {
        context.create().resource(FullIndexAuditServiceImpl.AUDIT_ROOT, "jcr:primaryType", "sling:Folder");
        context.create().resource(FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-1", "path", "/content/a");
        context.create().resource(FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-2", "path", "/content/b");

        final int removed = fullIndexAuditService.clearAllEvents();

        assertEquals(2, removed);
        assertTrue(context.resourceResolver().getResource(FullIndexAuditServiceImpl.AUDIT_ROOT).getChildren().iterator().hasNext() == false);
    }

    @Test
    void purgeOlderThanHours_removesStaleEvents() {
        context.create().resource(FullIndexAuditServiceImpl.AUDIT_ROOT, "jcr:primaryType", "sling:Folder");
        final Calendar stale = Calendar.getInstance();
        stale.add(Calendar.HOUR, -48);
        final Calendar recent = Calendar.getInstance();
        context.create().resource(
                FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-stale",
                "path", "/content/old",
                "timestamp", stale);
        context.create().resource(
                FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-recent",
                "path", "/content/new",
                "timestamp", recent);

        fullIndexAuditService.purgeOlderThanHours(24);

        assertEquals(
                "/content/new",
                context.resourceResolver()
                        .getResource(FullIndexAuditServiceImpl.AUDIT_ROOT)
                        .getChildren()
                        .iterator()
                        .next()
                        .getValueMap()
                        .get("path", String.class));
    }

    @Test
    void listEventsForReport_returnsAllWhenStatusFilterIsAll() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        context.create().resource(FullIndexAuditServiceImpl.AUDIT_ROOT, "jcr:primaryType", "sling:Folder");
        context.create().resource(
                FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-a",
                "path", "/content/a",
                "status", FullIndexAuditService.STATUS_SUCCESS,
                "action", FullIndexAuditService.ACTION_FULL_REINDEX,
                "message", "Indexed",
                "batchId", "batch-1",
                "eventKind", "BATCH",
                "timestamp", Calendar.getInstance());
        context.create().resource(
                FullIndexAuditServiceImpl.AUDIT_ROOT + "/event-b",
                "path", "/content/b",
                "status", FullIndexAuditService.STATUS_SUCCESS,
                "action", FullIndexAuditService.ACTION_FULL_REINDEX,
                "message", "Indexed",
                "batchId", "batch-2",
                "eventKind", "BATCH",
                "timestamp", Calendar.getInstance());

        final List<Map<String, Object>> events = fullIndexAuditService.listEventsForReport("ALL", 50, 24);

        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(event -> FullIndexAuditService.STATUS_SUCCESS.equals(event.get("status"))));
    }
}
