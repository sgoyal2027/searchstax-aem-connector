package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class FailedRequestServiceImplTest {

    private static final String FAILED_ROOT = "/var/searchstaxconnector/incremental-index/failed";

    private final AemContext context = AppAemContext.newAemContext();

    @InjectMocks
    private FailedRequestServiceImpl failedRequestService;

    @Mock
    private ResolverUtil resolverUtil;

    @BeforeEach
    void setup() throws Exception {
        context.create().resource(FAILED_ROOT, "jcr:primaryType", "sling:Folder");
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void saveFailedRequest_persistsFailureNode() {
        final IndexRequest request = new IndexRequest(
                "/content/wknd/en/page",
                ReplicationActionType.ACTIVATE,
                System.currentTimeMillis());
        request.setBatchId("batch-1");
        request.setRetryCount(2);

        final ApiResponse response = new ApiResponse(500, "{\"message\":\"Server error\"}");

        failedRequestService.saveFailedRequest(request, "PERMANENT_FAILURE", response);

        final Resource failedRoot = context.resourceResolver().getResource(FAILED_ROOT);
        assertNotNull(failedRoot);

        int childCount = 0;
        for (final Resource ignored : failedRoot.getChildren()) {
            childCount++;
        }
        assertEquals(1, childCount);

        final Resource saved = failedRoot.getChildren().iterator().next();
        assertEquals("/content/wknd/en/page", saved.getValueMap().get("path", String.class));
        assertEquals("batch-1", saved.getValueMap().get("batchId", String.class));
        assertEquals("PERMANENT_FAILURE", saved.getValueMap().get("failureReason", String.class));
        assertEquals(500, saved.getValueMap().get("statusCode", Integer.class));
        assertEquals(2, saved.getValueMap().get("retryCount", Integer.class));
        assertEquals(500, request.getStatusCode());
    }

    @Test
    void saveFailedRequest_handlesNullResponse() {
        final IndexRequest request = new IndexRequest(
                "/content/wknd/en/asset",
                ReplicationActionType.DELETE,
                System.currentTimeMillis());
        request.setBatchId("batch-2");

        failedRequestService.saveFailedRequest(request, "MAX_RETRY_COUNT_EXHAUSTED", null);

        final Resource failedRoot = context.resourceResolver().getResource(FAILED_ROOT);
        final Resource saved = failedRoot.getChildren().iterator().next();
        assertEquals("DELETE", saved.getValueMap().get("actionType", String.class));
        assertEquals(-1, saved.getValueMap().get("statusCode", Integer.class));
        assertEquals("N/A", saved.getValueMap().get("responseMessage", String.class));
    }
}
