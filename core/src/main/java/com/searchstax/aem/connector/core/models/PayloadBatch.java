package com.searchstax.aem.connector.core.models;

import com.searchstax.aem.connector.core.dto.request.IndexRequest;

import java.util.List;

public class PayloadBatch {

    public PayloadBatch(
            List<IndexRequest> requests,
            String payload,
            long payloadSize,
            String batchId) {

        this.requests = requests;
        this.payload = payload;
        this.payloadSize = payloadSize;
        this.batchId = batchId;
    }

    private List<IndexRequest> requests;

    private String payload;

    private long payloadSize;

    private String batchId;

    public List<IndexRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<IndexRequest> requests) {
        this.requests = requests;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public long getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(long payloadSize) {
        this.payloadSize = payloadSize;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}
