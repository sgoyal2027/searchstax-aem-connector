package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.services.MaintenanceModeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Component(service = MaintenanceModeService.class)
public class MaintenanceModeServiceImpl implements MaintenanceModeService {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceModeServiceImpl.class);

    private static final String DEFAULT_MESSAGE =
            "Search is temporarily unavailable. Please try again later.";

    private static final int DEFAULT_THRESHOLD = 3;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    @Reference
    private InitialSetupConfigService initialSetupConfigService;

    @Override
    public boolean isActive() {
        final InitialSetupConfig config = initialSetupConfigService.getConfiguration();
        if (config.isMaintenanceModeManual()) {
            return true;
        }
        final int threshold = config.getMaintenanceFailureThreshold() > 0
                ? config.getMaintenanceFailureThreshold()
                : DEFAULT_THRESHOLD;
        return consecutiveFailures.get() >= threshold;
    }

    @Override
    public String getMessage() {
        final InitialSetupConfig config = initialSetupConfigService.getConfiguration();
        final String message = config.getMaintenanceMessage();
        return message != null && !message.isBlank() ? message : DEFAULT_MESSAGE;
    }

    @Override
    public void recordHttpStatus(final int statusCode) {
        if (isRetryableServerError(statusCode)) {
            final int count = consecutiveFailures.incrementAndGet();
            LOG.warn("SearchStax server error HTTP {} — consecutive failures={}", statusCode, count);
        } else if (statusCode >= 200 && statusCode < 300) {
            resetFailures();
        }
    }

    @Override
    public void resetFailures() {
        consecutiveFailures.set(0);
    }

    static boolean isRetryableServerError(final int statusCode) {
        return statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504
                || statusCode == 599;
    }
}
