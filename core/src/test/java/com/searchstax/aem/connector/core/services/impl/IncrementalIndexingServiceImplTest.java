package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.services.DocumentValidationService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IndexingHelperService;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.services.PageDocumentBuilderService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementalIndexingServiceImplTest {

    @Mock
    private IncrementalQueueService queueService;

    @Mock
    private PageDocumentBuilderService pageDocumentBuilderService;

    @Mock
    private DocumentValidationService documentValidationService;

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private IndexingHelperService indexingHelperService;

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
    void processBatch_skipsOversizedDocumentsAndRemovesFromQueue() throws Exception {
        final IndexRequest request = new IndexRequest(
                "/content/wknd/en/large-page", ReplicationActionType.ACTIVATE, System.currentTimeMillis());
        request.setCorrelationId("job-oversize");

        final char[] padding = new char[SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES];
        java.util.Arrays.fill(padding, 'x');
        final String largeTitle = new String(padding) + "overflow";

        when(pageDocumentBuilderService.buildDocument(resourceResolver, request.getPath()))
                .thenReturn(Map.of("language_s", "en", "id", request.getPath(), "title_txt_en", largeTitle));
        when(documentValidationService.validateMandatoryFields(any(), eq("en")))
                .thenReturn(Optional.empty());

        incrementalIndexingService.processBatch(resourceResolver, List.of(request));

        verify(queueService).removeProcessed(List.of(request));
        verify(indexingAuditService).recordEvent(
                eq(request.getPath()),
                eq("ACTIVATE"),
                eq(IndexingAuditService.STATUS_SKIPPED),
                any(),
                eq("job-oversize"),
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
}
