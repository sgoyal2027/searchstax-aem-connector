package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.models.PayloadBatch;
import com.searchstax.aem.connector.core.services.AssetDocumentBuilderService;
import com.searchstax.aem.connector.core.services.DocumentValidationService;
import com.searchstax.aem.connector.core.services.FailedRequestService;
import com.searchstax.aem.connector.core.services.IndexFailureNotificationService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.services.PageDocumentBuilderService;
import com.searchstax.aem.connector.core.services.PayLoadBatchService;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementalIndexingServiceImplTest {

    @Mock
    private IncrementalQueueService queueService;

    @Mock
    private PageDocumentBuilderService pageDocumentBuilderService;

    @Mock
    private AssetDocumentBuilderService assetDocumentBuilderService;

    @Mock
    private DocumentValidationService documentValidationService;

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private IndexingHelperService indexingHelperService;

    @Mock
    private PayLoadBatchService payLoadBatchService;

    @Mock
    private SearchstaxClientService searchstaxClientService;

    @Mock
    private FailedRequestService failedRequestService;

    @Mock
    private IndexFailureNotificationService indexFailureNotificationService;

    @Mock
    private ResourceResolver resourceResolver;

    @InjectMocks
    private IncrementalIndexingServiceImpl incrementalIndexingService;

    @Test
    void processBatch_skipsMandatoryValidationFailures() throws Exception {
        final IndexRequest request = new IndexRequest(
                "/content/wknd/en/page", ReplicationActionType.ACTIVATE, System.currentTimeMillis());
        request.setCorrelationId("job-123");

        when(pageDocumentBuilderService.buildDocument(resourceResolver, request.getPath()))
                .thenReturn(Map.of("language_s", "en", "id", request.getPath()));
        when(documentValidationService.validateMandatoryFields(any(), eq("en")))
                .thenReturn(Optional.of("MISSING_MANDATORY_FIELD: title_txt_en"));

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("ACTIVATE"),
                eq(IndexingAuditService.STATUS_SKIPPED),
                any(),
                eq("job-123"),
                eq(0L),
                eq(null));
    }

    @Test
    void processBatch_doesNotRemoveWhenBuildThrowsBeforeSuccess() throws Exception {
        final IndexRequest request = new IndexRequest(
                "/content/wknd/en/page", ReplicationActionType.ACTIVATE, System.currentTimeMillis());

        when(pageDocumentBuilderService.buildDocument(resourceResolver, request.getPath()))
                .thenThrow(new IllegalStateException("boom"));
        when(indexingHelperService.calculateDelay(1)).thenReturn(0L);

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(queueService, never()).removeProcessed(any());
        verify(queueService).updateRequest(request);
    }

    @Test
    void processBatch_recordsSuccessAndRemovesFromQueue() throws Exception {
        final IndexRequest request = activateRequest("/content/wknd/en/page", "job-success");
        stubValidDocument(request);
        final PayloadBatch payloadBatch = payloadBatch(request, "{}");
        when(payLoadBatchService.buildIndexBatches(any(), any())).thenReturn(List.of(payloadBatch));
        when(searchstaxClientService.indexDocument(anyString(), eq(request.getPath())))
                .thenReturn(new ApiResponse(200, "ok"));
        when(indexingHelperService.isSuccess(any())).thenReturn(true);

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("ACTIVATE"),
                eq(IndexingAuditService.STATUS_SUCCESS),
                eq("Indexed"),
                eq("job-success"),
                any(Long.class),
                isNull());
    }

    @Test
    void processBatch_permanentFailure_savesFailedRequestAndRemovesFromQueue() throws Exception {
        final IndexRequest request = activateRequest("/content/wknd/en/page", "job-permanent");
        stubValidDocument(request);
        final PayloadBatch payloadBatch = payloadBatch(request, "{}");
        final ApiResponse apiResponse = new ApiResponse(401, "unauthorized");
        when(payLoadBatchService.buildIndexBatches(any(), any())).thenReturn(List.of(payloadBatch));
        when(searchstaxClientService.indexDocument(anyString(), eq(request.getPath()))).thenReturn(apiResponse);
        when(indexingHelperService.isSuccess(any())).thenReturn(false);
        when(indexingHelperService.isPlanLimitExceeded(any())).thenReturn(false);
        when(indexingHelperService.isPermanentFailure(any())).thenReturn(true);
        when(indexingHelperService.formatFailureMessage(eq("PERMANENT_FAILURE"), eq(apiResponse), isNull()))
                .thenReturn("readable permanent failure");

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(failedRequestService).saveFailedRequest(request, "PERMANENT_FAILURE", apiResponse);
        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("ACTIVATE"),
                eq(IndexingAuditService.STATUS_FAILURE),
                eq("readable permanent failure"),
                eq("job-permanent"),
                eq(0L),
                isNull());
        verify(indexFailureNotificationService).sendFailureNotification(isNull(), eq(List.of(request)));
    }

    @Test
    void processBatch_planLimitExceeded_savesFailedRequestAndRemovesFromQueue() throws Exception {
        final IndexRequest request = activateRequest("/content/wknd/en/page", "job-plan");
        stubValidDocument(request);
        final PayloadBatch payloadBatch = payloadBatch(request, "{}");
        final ApiResponse apiResponse = new ApiResponse(429, "Plan Limit Exceeded");
        when(payLoadBatchService.buildIndexBatches(any(), any())).thenReturn(List.of(payloadBatch));
        when(searchstaxClientService.indexDocument(anyString(), eq(request.getPath()))).thenReturn(apiResponse);
        when(indexingHelperService.isSuccess(any())).thenReturn(false);
        when(indexingHelperService.isPlanLimitExceeded(any())).thenReturn(true);
        when(indexingHelperService.formatFailureMessage(eq("PLAN_LIMIT_EXCEEDED"), eq(apiResponse), isNull()))
                .thenReturn("readable plan limit");

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(failedRequestService).saveFailedRequest(request, "PLAN_LIMIT_EXCEEDED", apiResponse);
        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("ACTIVATE"),
                eq(IndexingAuditService.STATUS_FAILURE),
                eq("readable plan limit"),
                eq("job-plan"),
                eq(0L),
                isNull());
    }

    @Test
    void processBatch_exhaustsRetriesAfterFiveAttempts() throws Exception {
        final IndexRequest request = activateRequest("/content/wknd/en/page", "job-retry");
        stubValidDocument(request);
        final PayloadBatch payloadBatch = payloadBatch(request, "{}");
        final ApiResponse apiResponse = new ApiResponse(503, "unavailable");
        when(payLoadBatchService.buildIndexBatches(any(), any())).thenReturn(List.of(payloadBatch));
        when(searchstaxClientService.indexDocument(anyString(), eq(request.getPath()))).thenReturn(apiResponse);
        when(indexingHelperService.isSuccess(any())).thenReturn(false);
        when(indexingHelperService.isPlanLimitExceeded(any())).thenReturn(false);
        when(indexingHelperService.isPermanentFailure(any())).thenReturn(false);
        when(indexingHelperService.calculateDelay(anyInt())).thenReturn(0L);
        when(indexingHelperService.formatFailureMessage(eq("MAX_RETRY_COUNT_EXHAUSTED"), eq(apiResponse), isNull()))
                .thenReturn("retry exhausted");

        for (int attempt = 0; attempt < SearchStaxIndexingLimits.MAX_INDEXING_RETRIES; attempt++) {
            incrementalIndexingService.processBatch(resourceResolver, List.of(request));
            verify(queueService, times(attempt + 1)).updateRequest(request);
        }

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(failedRequestService).saveFailedRequest(request, "MAX_RETRY_COUNT_EXHAUSTED", apiResponse);
        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("ACTIVATE"),
                eq(IndexingAuditService.STATUS_FAILURE),
                eq("retry exhausted"),
                eq("job-retry"),
                eq(0L),
                isNull());
    }

    @Test
    void processBatch_usesAssetBuilderForDamPaths() throws Exception {
        final IndexRequest request = activateRequest("/content/dam/wknd/en/image.jpg", "job-asset");
        when(assetDocumentBuilderService.buildDocument(resourceResolver, request.getPath()))
                .thenReturn(Map.of("language_s", "en", "id", request.getPath(), "title_txt_en", "Asset"));
        when(documentValidationService.validateMandatoryFields(any(), eq("en"))).thenReturn(Optional.empty());
        final PayloadBatch payloadBatch = payloadBatch(request, "{}");
        when(payLoadBatchService.buildIndexBatches(any(), any())).thenReturn(List.of(payloadBatch));
        when(searchstaxClientService.indexDocument(anyString(), eq(request.getPath())))
                .thenReturn(new ApiResponse(200, "ok"));
        when(indexingHelperService.isSuccess(any())).thenReturn(true);

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(assetDocumentBuilderService).buildDocument(resourceResolver, request.getPath());
        verify(pageDocumentBuilderService, never()).buildDocument(any(), anyString());
        verify(queueService).removeProcessed(List.of(request));
    }

    @Test
    void processBatch_skipsNullDocumentAsUnsupported() throws Exception {
        final IndexRequest request = activateRequest("/content/wknd/en/page", "job-null");
        when(pageDocumentBuilderService.buildDocument(resourceResolver, request.getPath())).thenReturn(null);

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("ACTIVATE"),
                eq(IndexingAuditService.STATUS_SKIPPED),
                eq("Unsupported or missing document"),
                eq("job-null"),
                eq(0L),
                isNull());
    }

    @Test
    void processBatch_deleteSuccessRemovesFromQueue() throws Exception {
        final IndexRequest request = new IndexRequest(
                "/content/wknd/en/page", ReplicationActionType.DELETE, System.currentTimeMillis());
        request.setCorrelationId("job-delete");
        final PayloadBatch deleteBatch = payloadBatch(request, "{\"delete\":true}");
        when(payLoadBatchService.buildDeleteBatches(any(), any())).thenReturn(List.of(deleteBatch));
        when(searchstaxClientService.deleteDocument(anyString(), eq(request.getPath())))
                .thenReturn(new ApiResponse(200, "ok"));
        when(indexingHelperService.isSuccess(any())).thenReturn(true);

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("DELETE"),
                eq(IndexingAuditService.STATUS_SUCCESS),
                eq("Deleted"),
                eq("job-delete"),
                any(Long.class),
                isNull());
    }

    @Test
    void processBatch_deletePermanentFailure_savesFailedRequest() throws Exception {
        final IndexRequest request = new IndexRequest(
                "/content/wknd/en/page", ReplicationActionType.DEACTIVATE, System.currentTimeMillis());
        request.setCorrelationId("job-deactivate");
        final PayloadBatch deleteBatch = payloadBatch(request, "{\"delete\":true}");
        final ApiResponse apiResponse = new ApiResponse(404, "not found");
        when(payLoadBatchService.buildDeleteBatches(any(), any())).thenReturn(List.of(deleteBatch));
        when(searchstaxClientService.deleteDocument(anyString(), eq(request.getPath()))).thenReturn(apiResponse);
        when(indexingHelperService.isSuccess(any())).thenReturn(false);
        when(indexingHelperService.isPlanLimitExceeded(any())).thenReturn(false);
        when(indexingHelperService.isPermanentFailure(any())).thenReturn(true);
        when(indexingHelperService.formatFailureMessage(eq("DELETE_PERMANENT_FAILURE"), eq(apiResponse), isNull()))
                .thenReturn("delete permanent");

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(failedRequestService).saveFailedRequest(request, "DELETE_PERMANENT_FAILURE", apiResponse);
        verify(queueService).removeProcessed(List.of(request));
    }

    private static IndexRequest activateRequest(final String path, final String correlationId) {
        final IndexRequest request = new IndexRequest(path, ReplicationActionType.ACTIVATE, System.currentTimeMillis());
        request.setCorrelationId(correlationId);
        return request;
    }

    private void stubValidDocument(final IndexRequest request) throws Exception {
        when(pageDocumentBuilderService.buildDocument(resourceResolver, request.getPath()))
                .thenReturn(Map.of("language_s", "en", "id", request.getPath(), "title_txt_en", "Title"));
        when(documentValidationService.validateMandatoryFields(any(), eq("en")))
                .thenReturn(Optional.empty());
    }

    private static PayloadBatch payloadBatch(final IndexRequest request, final String payload) {
        return new PayloadBatch(List.of(request), payload, payload.length(), "batch-1");
    }
}
