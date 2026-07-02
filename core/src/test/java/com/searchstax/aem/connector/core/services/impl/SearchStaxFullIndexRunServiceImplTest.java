package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexOrchestratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxFullIndexRunServiceImplTest {

    @InjectMocks
    private SearchStaxFullIndexRunServiceImpl service;

    @Mock
    private SearchStaxFullIndexOrchestratorService orchestratorService;

    @Test
    void testTriggerFullIndex() {

        FullIndexPathConfig config =
                new FullIndexPathConfig(
                        new String[]{"/content"},
                        new String[]{"/content/site"},
                        new boolean[]{true},
                        new String[]{"/content/site/excluded"});

        FullIndexTriggerResult triggerResult =
                new FullIndexTriggerResult(
                        true,
                        "job-1",
                        "Accepted",
                        202);

        when(orchestratorService.triggerFullIndex(config))
                .thenReturn(triggerResult);

        FullIndexTriggerResult result =
                service.triggerFullIndex(config);

        assertSame(triggerResult, result);

        verify(orchestratorService).triggerFullIndex(config);
    }

    @Test
    void testGetProgress() {

        FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.SUCCESS,
                        100,
                        100,
                        0,
                        80,
                        20,
                        5,
                        "/content/site",
                        System.currentTimeMillis(),
                        1000,
                        "Completed");

        when(orchestratorService.getProgress())
                .thenReturn(progress);

        FullIndexProgress result =
                service.getProgress();

        assertSame(progress, result);

        verify(orchestratorService).getProgress();
    }
}