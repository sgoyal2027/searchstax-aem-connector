package com.searchstax.aem.connector.core.dto.request;

import com.day.cq.replication.ReplicationActionType;

public class IndexRequest {

    private String batchId;

    private String path;

    private ReplicationActionType actionType;

    private long eventTime;

    private int retryCount;

    private String correlationId;

    private int statusCode;

    private String responseMessage;

    public IndexRequest() {
    }

    public IndexRequest(String path,
                        ReplicationActionType actionType,
                        long eventTime) {

        this.path = path;
        this.actionType = actionType;
        this.eventTime = eventTime;
        this.retryCount = 0;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ReplicationActionType getActionType() {
        return actionType;
    }

    public void setActionType(ReplicationActionType actionType) {
        this.actionType = actionType;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(final String responseMessage) {
        this.responseMessage = responseMessage;
    }
    
}
