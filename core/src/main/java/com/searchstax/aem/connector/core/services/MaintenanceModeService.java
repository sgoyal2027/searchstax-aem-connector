package com.searchstax.aem.connector.core.services;

/**
 * Tracks SearchStax availability for maintenance mode (SRS 5.9).
 */
public interface MaintenanceModeService {

    boolean isActive();

    String getMessage();

    /** Records an HTTP status from a SearchStax update/select call. */
    void recordHttpStatus(int statusCode);

    void resetFailures();
}
