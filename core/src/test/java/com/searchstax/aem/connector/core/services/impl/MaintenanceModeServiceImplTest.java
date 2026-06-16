package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceModeServiceImplTest {

    @Mock
    private InitialSetupConfigService initialSetupConfigService;

    @InjectMocks
    private MaintenanceModeServiceImpl maintenanceModeService;

    private InitialSetupConfig config;

    @BeforeEach
    void setUp() {
        config = new InitialSetupConfig();
        config.setMaintenanceFailureThreshold(3);
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);
    }

    @Test
    void isActive_whenManualMaintenanceEnabled() {
        config.setMaintenanceModeManual(true);
        assertTrue(maintenanceModeService.isActive());
    }

    @Test
    void isActive_afterConsecutiveServerErrors() {
        maintenanceModeService.recordHttpStatus(500);
        maintenanceModeService.recordHttpStatus(503);
        assertFalse(maintenanceModeService.isActive());
        maintenanceModeService.recordHttpStatus(504);
        assertTrue(maintenanceModeService.isActive());
    }

    @Test
    void resetFailures_onSuccessStatus() {
        maintenanceModeService.recordHttpStatus(500);
        maintenanceModeService.recordHttpStatus(500);
        maintenanceModeService.recordHttpStatus(200);
        maintenanceModeService.recordHttpStatus(500);
        assertFalse(maintenanceModeService.isActive());
    }
}
