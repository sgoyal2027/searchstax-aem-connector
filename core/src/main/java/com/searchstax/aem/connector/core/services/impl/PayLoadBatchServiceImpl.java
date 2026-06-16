package com.searchstax.aem.connector.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.constants.SearchStaxIndexingLimits;
import com.searchstax.aem.connector.core.dto.request.IndexRequest;
import com.searchstax.aem.connector.core.models.PayloadBatch;
import com.searchstax.aem.connector.core.services.PayLoadBatchService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component(service = PayLoadBatchService.class)
public class PayLoadBatchServiceImpl implements PayLoadBatchService {

    private static final Logger LOG = LoggerFactory.getLogger(PayLoadBatchServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public List<PayloadBatch> buildIndexBatches(
            final List<IndexRequest> requests,
            final List<Map<String, Object>> documents) throws Exception {

        if (requests.size() != documents.size()) {
            throw new IllegalArgumentException(
                    "Requests and documents size mismatch. Requests="
                            + requests.size()
                            + ", Documents="
                            + documents.size());
        }

        LOG.info("Building payload batches. Requests={} Documents={}", requests.size(), documents.size());

        final List<PayloadBatch> batches = new ArrayList<>();
        List<IndexRequest> currentRequests = new ArrayList<>();
        List<Map<String, Object>> currentDocuments = new ArrayList<>();
        long currentPayloadSize = 0;

        for (int i = 0; i < documents.size(); i++) {
            final IndexRequest request = requests.get(i);
            final Map<String, Object> document = documents.get(i);
            final long documentSize = getDocumentSize(document);

            if (documentSize > SearchStaxIndexingLimits.MAX_DOCUMENT_BYTES) {
                LOG.warn(
                        "Skipping document exceeding SRS 10 KB limit. Path={} Size={} bytes",
                        request.getPath(),
                        documentSize);
                continue;
            }

            if (documentSize > SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES) {
                LOG.warn(
                        "Single document exceeds maximum batch payload size. Path={} Size={} KB",
                        request.getPath(),
                        documentSize / 1024);
                continue;
            }

            final boolean batchCountExceeded =
                    currentDocuments.size() >= SearchStaxIndexingLimits.MAX_BATCH_DOCUMENT_COUNT;
            final boolean payloadExceeded =
                    currentPayloadSize + documentSize > SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES;

            if (!currentDocuments.isEmpty() && (batchCountExceeded || payloadExceeded)) {
                batches.add(createBatch(currentRequests, currentDocuments));
                currentRequests = new ArrayList<>();
                currentDocuments = new ArrayList<>();
                currentPayloadSize = 0;
            }

            currentRequests.add(request);
            currentDocuments.add(document);
            currentPayloadSize += documentSize;
        }

        if (!currentDocuments.isEmpty()) {
            batches.add(createBatch(currentRequests, currentDocuments));
        }

        LOG.info("Payload batch creation completed. Total Batches={}", batches.size());
        return batches;
    }

    @Override
    public List<PayloadBatch> buildDeleteBatches(final List<IndexRequest> requests, final List<String> ids)
            throws Exception {

        LOG.info("Building delete batches. Requests={}", requests.size());

        final List<PayloadBatch> batches = new ArrayList<>();
        List<IndexRequest> currentRequests = new ArrayList<>();
        List<String> currentIds = new ArrayList<>();
        long currentPayloadSize = 0;

        for (int i = 0; i < ids.size(); i++) {
            final String id = ids.get(i);
            final IndexRequest request = requests.get(i);
            final long documentSize = getDeleteDocumentSize(id);

            final boolean batchCountExceeded =
                    currentIds.size() >= SearchStaxIndexingLimits.MAX_BATCH_DOCUMENT_COUNT;
            final boolean payloadExceeded =
                    currentPayloadSize + documentSize > SearchStaxIndexingLimits.MAX_BATCH_PAYLOAD_BYTES;

            if (!currentIds.isEmpty() && (batchCountExceeded || payloadExceeded)) {
                batches.add(createDeleteBatch(currentRequests, currentIds));
                currentRequests = new ArrayList<>();
                currentIds = new ArrayList<>();
                currentPayloadSize = 0;
            }

            currentRequests.add(request);
            currentIds.add(id);
            currentPayloadSize += documentSize;
        }

        if (!currentIds.isEmpty()) {
            batches.add(createDeleteBatch(currentRequests, currentIds));
        }

        LOG.info("Delete batch creation completed. Total Batches={}", batches.size());
        return batches;
    }

    private PayloadBatch createBatch(
            final List<IndexRequest> requests, final List<Map<String, Object>> documents) throws Exception {

        final String batchId = UUID.randomUUID().toString();
        for (final IndexRequest request : requests) {
            request.setBatchId(batchId);
        }

        final String payload = OBJECT_MAPPER.writeValueAsString(documents);
        final long payloadSize = payload.getBytes(StandardCharsets.UTF_8).length;

        LOG.info(
                "Payload batch created. Batch ID={} Documents={} Payload Size={} KB",
                batchId,
                requests.size(),
                payloadSize / 1024);

        return new PayloadBatch(new ArrayList<>(requests), payload, payloadSize, batchId);
    }

    private long getDocumentSize(final Map<String, Object> document) throws Exception {
        return OBJECT_MAPPER.writeValueAsBytes(document).length;
    }

    private PayloadBatch createDeleteBatch(final List<IndexRequest> requests, final List<String> ids)
            throws Exception {

        final List<Map<String, String>> deleteEntries = new ArrayList<>();
        for (final String id : ids) {
            final Map<String, String> deleteEntry = new HashMap<>();
            deleteEntry.put("id", id);
            deleteEntries.add(deleteEntry);
        }

        final String batchId = UUID.randomUUID().toString();
        for (final IndexRequest request : requests) {
            request.setBatchId(batchId);
        }

        final Map<String, Object> deleteRequest = new HashMap<>();
        deleteRequest.put("delete", deleteEntries);

        final String payload = OBJECT_MAPPER.writeValueAsString(deleteRequest);
        final long payloadSize = payload.getBytes(StandardCharsets.UTF_8).length;

        LOG.info(
                "Delete batch created. Batch ID={} Documents={} Payload Size={} KB",
                batchId,
                requests.size(),
                String.format("%.2f", payloadSize / 1024.0));

        return new PayloadBatch(new ArrayList<>(requests), payload, payloadSize, batchId);
    }

    private long getDeleteDocumentSize(final String id) throws Exception {
        final Map<String, String> deleteEntry = new HashMap<>();
        deleteEntry.put("id", id);
        return OBJECT_MAPPER.writeValueAsBytes(deleteEntry).length;
    }
}
