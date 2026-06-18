package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.replication.ReplicationActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.searchstax.aem.connector.core.services.IncrementalIndexingService;
import com.searchstax.aem.connector.core.services.IncrementalQueueService;
import com.searchstax.aem.connector.core.services.PageDocumentBuilderService;
import com.searchstax.aem.connector.core.services.PayLoadBatchService;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component(service = IncrementalIndexingService.class)
public class IncrementalIndexingServiceImpl implements IncrementalIndexingService {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalIndexingServiceImpl.class);

    private static final int MAX_RETRY_COUNT = 5;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Reference
    private IncrementalQueueService queueService;

    @Reference
    private PageDocumentBuilderService pageDocumentBuilderService;

    @Reference
    private AssetDocumentBuilderService assetDocumentBuilderService;

    @Reference
    private PayLoadBatchService payLoadBatchService;

    @Reference
    private SearchstaxClientService searchstaxClientService;

    @Reference
    private IndexingHelperService indexingHelperService;

    @Reference
    private FailedRequestService failedRequestService;

    @Reference
    private IndexFailureNotificationService indexFailureNotificationService;

    @Reference
    private DocumentValidationService documentValidationService;

    @Reference
    private IndexingAuditService indexingAuditService;

    @Override
    public void processBatch(final ResourceResolver resolver, final List<IndexRequest> batch) {
        final List<IndexRequest> successfulRequests = new ArrayList<>();
        final List<IndexRequest> failedRequests = new ArrayList<>();

        try {
            final List<IndexRequest> activateRequests = new ArrayList<>();
            final List<IndexRequest> deleteRequests = new ArrayList<>();

            for (final IndexRequest request : batch) {
                if (request.getActionType() == ReplicationActionType.ACTIVATE) {
                    activateRequests.add(request);
                } else {
                    deleteRequests.add(request);
                }
            }

            LOG.info("Processing batch. Activates={} Deletes={}", activateRequests.size(), deleteRequests.size());

            processActivates(resolver, activateRequests, successfulRequests, failedRequests);
            processDeletes(deleteRequests, successfulRequests, failedRequests);

            if (!failedRequests.isEmpty()) {
                indexFailureNotificationService.sendFailureNotification(
                        failedRequests.get(0).getBatchId(),
                        failedRequests);
            }

            LOG.info(
                    "Processed batch size={} success={} remaining={}",
                    batch.size(),
                    successfulRequests.size(),
                    queueService.size());
        } catch (Exception e) {
            LOG.error("Error processing batch", e);
        } finally {
            if (!successfulRequests.isEmpty()) {
                queueService.removeProcessed(successfulRequests);
            }
        }
    }

    private void processActivates(
            final ResourceResolver resolver,
            final List<IndexRequest> activateRequests,
            final List<IndexRequest> successfulRequests,
            final List<IndexRequest> failedRequests) {

        if (activateRequests.isEmpty()) {
            return;
        }

        final List<IndexRequest> validRequests = new ArrayList<>();
        final List<Map<String, Object>> documents = new ArrayList<>();

        for (final IndexRequest request : activateRequests) {
            try {
                final Map<String, Object> document;
                if (request.getPath().startsWith("/content/dam")) {
                    document = assetDocumentBuilderService.buildDocument(resolver, request.getPath());
                } else {
                    document = pageDocumentBuilderService.buildDocument(resolver, request.getPath());
                }

                if (document != null) {
                    final String language = String.valueOf(document.getOrDefault("language_s", "en"));
                    final Optional<String> validationError =
                            documentValidationService.validateMandatoryFields(document, language);
                    if (validationError.isPresent()) {
                        LOG.info("Skipping path due to mandatory field validation. Path={} Reason={}",
                                request.getPath(), validationError.get());
                        auditEvent(request, IndexingAuditService.STATUS_SKIPPED, validationError.get(), 0);
                        successfulRequests.add(request);
                        continue;
                    }

                    final long documentSize = OBJECT_MAPPER.writeValueAsBytes(document).length;
                    if (documentSize > SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES) {
                        final String reason = String.format(
                                "Document payload %d bytes exceeds SRS %d byte limit",
                                documentSize,
                                SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES);
                        LOG.warn("Skipping document exceeding SRS 10 KB limit. Path={} Size={} bytes",
                                request.getPath(), documentSize);
                        auditEvent(request, IndexingAuditService.STATUS_SKIPPED, reason, 0);
                        successfulRequests.add(request);
                        continue;
                    }

                    documents.add(document);
                    validRequests.add(request);
                } else {
                    LOG.info("Skipping unsupported asset. Path={}", request.getPath());
                    auditEvent(request, IndexingAuditService.STATUS_SKIPPED, "Unsupported or missing document", 0);
                    successfulRequests.add(request);
                }
            } catch (Exception e) {
                handleRetryableFailure(
                        request,
                        successfulRequests,
                        failedRequests,
                        "MAX_RETRY_COUNT_REACHED",
                        "Failed building document. Path={} Retry={}",
                        e);
            }
        }

        if (validRequests.isEmpty()) {
            return;
        }

        List<PayloadBatch> payloadBatches;
        try {
            payloadBatches = payLoadBatchService.buildIndexBatches(validRequests, documents);
        } catch (Exception e) {
            LOG.error("Failed to build index payload batches", e);
            for (final IndexRequest request : validRequests) {
                handleRetryableFailure(
                        request,
                        successfulRequests,
                        failedRequests,
                        "MAX_RETRY_COUNT_EXHAUSTED",
                        "Payload batch build failed for path={} Retry={}",
                        e);
            }
            return;
        }

        LOG.info("Created {} payload batches", payloadBatches.size());

        for (final PayloadBatch payloadBatch : payloadBatches) {
            final long batchStart = System.currentTimeMillis();
            final String routingPath = payloadBatch.getRequests().get(0).getPath();
            final ApiResponse response =
                    searchstaxClientService.indexDocument(payloadBatch.getPayload(), routingPath);
            final long duration = System.currentTimeMillis() - batchStart;

            if (indexingHelperService.isSuccess(response)) {
                successfulRequests.addAll(payloadBatch.getRequests());
                for (final IndexRequest request : payloadBatch.getRequests()) {
                    auditEvent(request, IndexingAuditService.STATUS_SUCCESS, successMessage(request.getActionType()), duration);
                }
                LOG.info(
                        "Successfully indexed payload batch. Documents={} Size={} KB",
                        payloadBatch.getRequests().size(),
                        payloadBatch.getPayloadSize() / 1024);
            } else if (indexingHelperService.isPlanLimitExceeded(response)) {
                for (final IndexRequest request : payloadBatch.getRequests()) {
                    failedRequestService.saveFailedRequest(request, "PLAN_LIMIT_EXCEEDED", response);
                    failedRequests.add(request);
                    auditEvent(request, IndexingAuditService.STATUS_FAILURE, "PLAN_LIMIT_EXCEEDED", 0);
                }
                LOG.error(
                        "SearchStax plan limit exceeded. Removing {} requests from queue.",
                        payloadBatch.getRequests().size());
                successfulRequests.addAll(payloadBatch.getRequests());
            } else if (indexingHelperService.isPermanentFailure(response)) {
                LOG.error(
                        "Non-retryable error detected. Status={} Removing {} requests from queue.",
                        response != null ? response.getStatusCode() : "NULL",
                        payloadBatch.getRequests().size());

                for (final IndexRequest request : payloadBatch.getRequests()) {
                    failedRequestService.saveFailedRequest(request, "PERMANENT_FAILURE", response);
                    failedRequests.add(request);
                    auditEvent(request, IndexingAuditService.STATUS_FAILURE, "PERMANENT_FAILURE", 0);
                    LOG.error("Removing request due to permanent failure. Path={}", request.getPath());
                }
                successfulRequests.addAll(payloadBatch.getRequests());
            } else {
                for (final IndexRequest request : payloadBatch.getRequests()) {
                    handleRetryableFailure(
                            request,
                            successfulRequests,
                            failedRequests,
                            "MAX_RETRY_COUNT_EXHAUSTED",
                            "Retry attempt {} for path={}",
                            null);
                }
            }
        }
    }

    private void processDeletes(
            final List<IndexRequest> deleteRequests,
            final List<IndexRequest> successfulRequests,
            final List<IndexRequest> failedRequests) {

        if (deleteRequests.isEmpty()) {
            return;
        }

        final List<String> deleteIds =
                deleteRequests.stream().map(IndexRequest::getPath).collect(Collectors.toList());

        List<PayloadBatch> deleteBatches;
        try {
            deleteBatches = payLoadBatchService.buildDeleteBatches(deleteRequests, deleteIds);
        } catch (Exception e) {
            LOG.error("Failed to build delete payload batches", e);
            for (final IndexRequest request : deleteRequests) {
                handleRetryableFailure(
                        request,
                        successfulRequests,
                        failedRequests,
                        "DELETE_RETRY_EXHAUSTED",
                        "Delete payload batch build failed for path={} Retry={}",
                        e);
            }
            return;
        }

        LOG.info("Created {} delete batches", deleteBatches.size());

        for (final PayloadBatch deleteBatch : deleteBatches) {
            try {
                final long batchStart = System.currentTimeMillis();
                final String routingPath = deleteBatch.getRequests().get(0).getPath();
                final ApiResponse response =
                        searchstaxClientService.deleteDocument(deleteBatch.getPayload(), routingPath);
                final long duration = System.currentTimeMillis() - batchStart;

                if (indexingHelperService.isSuccess(response)) {
                    successfulRequests.addAll(deleteBatch.getRequests());
                    for (final IndexRequest request : deleteBatch.getRequests()) {
                        auditEvent(request, IndexingAuditService.STATUS_SUCCESS, successMessage(request.getActionType()), duration);
                    }
                    LOG.info(
                            "Successfully deleted batch. Documents={} Size={} KB",
                            deleteBatch.getRequests().size(),
                            deleteBatch.getPayloadSize() / 1024);
                } else if (indexingHelperService.isPlanLimitExceeded(response)
                        || indexingHelperService.isPermanentFailure(response)) {
                    LOG.error(
                            "Permanent delete failure detected. Status={}",
                            response != null ? response.getStatusCode() : "NULL");

                    for (final IndexRequest request : deleteBatch.getRequests()) {
                        failedRequestService.saveFailedRequest(request, "DELETE_PERMANENT_FAILURE", response);
                        failedRequests.add(request);
                        auditEvent(request, IndexingAuditService.STATUS_FAILURE, "DELETE_PERMANENT_FAILURE", 0);
                    }
                    successfulRequests.addAll(deleteBatch.getRequests());
                } else {
                    for (final IndexRequest request : deleteBatch.getRequests()) {
                        handleRetryableFailure(
                                request,
                                successfulRequests,
                                failedRequests,
                                "DELETE_RETRY_EXHAUSTED",
                                "Retry attempt {} for path={}",
                                null);
                    }
                }
            } catch (Exception e) {
                LOG.error("Bulk delete processing failed", e);
                for (final IndexRequest request : deleteBatch.getRequests()) {
                    handleRetryableFailure(
                            request,
                            successfulRequests,
                            failedRequests,
                            "DELETE_RETRY_EXHAUSTED",
                            "Delete retry attempt {} for path={}",
                            e);
                }
            }
        }
    }

    private void handleRetryableFailure(
            final IndexRequest request,
            final List<IndexRequest> successfulRequests,
            final List<IndexRequest> failedRequests,
            final String exhaustedReason,
            final String retryLogPattern,
            final Exception cause) {

        request.setRetryCount(request.getRetryCount() + 1);

        if (cause != null) {
            LOG.error(retryLogPattern, request.getPath(), request.getRetryCount(), cause);
        } else {
            LOG.warn(retryLogPattern, request.getRetryCount(), request.getPath());
        }

        if (request.getRetryCount() >= MAX_RETRY_COUNT) {
            failedRequestService.saveFailedRequest(request, exhaustedReason, null);
            failedRequests.add(request);
            auditEvent(request, IndexingAuditService.STATUS_FAILURE, exhaustedReason, 0);
            LOG.error("Maximum retry count reached. Removing request from queue. Path={}", request.getPath());
            successfulRequests.add(request);
        } else {
            sleepBackoff(request.getRetryCount());
            queueService.updateRequest(request);
        }
    }

    private static String successMessage(final ReplicationActionType actionType) {
        if (actionType == ReplicationActionType.ACTIVATE) {
            return "Indexed";
        }
        if (actionType == ReplicationActionType.DEACTIVATE) {
            return "Deactivated";
        }
        return "Deleted";
    }

    private void auditEvent(
            final IndexRequest request,
            final String status,
            final String message,
            final long durationMs) {

        indexingAuditService.recordEvent(
                request.getPath(),
                request.getActionType() != null ? request.getActionType().name() : "",
                status,
                message,
                request.getCorrelationId(),
                durationMs,
                request.getBatchId());
    }

    private void sleepBackoff(final int retryCount) {
        try {
            Thread.sleep(indexingHelperService.calculateDelay(retryCount));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Incremental indexing backoff interrupted", e);
        }
    }
}
