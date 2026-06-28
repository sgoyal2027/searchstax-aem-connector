package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexExecutionService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexPathConfigurationService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchStaxFullIndexOrchestratorServiceImplTest {

    @InjectMocks
    private SearchStaxFullIndexOrchestratorServiceImpl service;

    @Mock
    private JobManager jobManager;

    @Mock
    private SlingSettingsService slingSettings;

    @Mock
    private InitialSetupConfigService initialSetupConfigService;

    @Mock
    private SearchStaxFullIndexExecutionService executionService;

    @Mock
    private SearchStaxFullIndexPathConfigurationService pathConfigurationService;

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource resource;

    @Mock
    private Job job;

    private InitialSetupConfig setupConfig;

    @BeforeEach
    void setup() {

        setupConfig = new InitialSetupConfig();
        setupConfig.setEnableConnector(true);

        lenient().when(slingSettings.getRunModes())
                .thenReturn(Set.of("author"));

        lenient().when(initialSetupConfigService.getConfiguration())
                .thenReturn(setupConfig);
    }

    private FullIndexPathConfig createConfig() {

        return new FullIndexPathConfig(
                "/content",
                new String[]{"/content/site"},
                new boolean[]{true},
                new String[0]);
    }

    private void mockNoRunningJobs() {

        doReturn(Collections.emptyList())
                .when(jobManager)
                .findJobs(
                        eq(JobManager.QueryType.ACTIVE),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull());

        doReturn(Collections.emptyList())
                .when(jobManager)
                .findJobs(
                        eq(JobManager.QueryType.QUEUED),
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        eq(-1L),
                        Mockito.<Map<String, Object>[]>isNull());
    }

    private void mockValidPaths() throws LoginException {

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content"))
                .thenReturn(resource);

        when(resolver.getResource("/content/site"))
                .thenReturn(resource);

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[]{"/content/site"});
    }

    @Test
    void testGetProgress() {

        FullIndexProgress progress =
                new FullIndexProgress(
                        FullIndexProgress.State.SUCCESS,
                        10,
                        10,
                        0,
                        10,
                        0,
                        0,
                        "/content/site",
                        1L,
                        100L,
                        "Completed");

        when(executionService.getProgressSnapshot())
                .thenReturn(progress);

        assertSame(progress, service.getProgress());

        verify(executionService)
                .getProgressSnapshot();
    }

    @Test
    void testRejectOnPublish() {

        when(slingSettings.getRunModes())
                .thenReturn(Set.of("publish"));

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
    }

    @Test
    void testRejectWhenConnectorDisabled() {

        setupConfig.setEnableConnector(false);

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertEquals("SearchStax connector is disabled.",
                result.getMessage());
    }

    @Test
    void testRejectWhenActiveJobExists() {

        when(jobManager.findJobs(
                eq(JobManager.QueryType.ACTIVE),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                ArgumentMatchers.<Map<String, Object>[]>any()))
                .thenReturn(Collections.singleton(job));

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(409, result.getHttpStatus());
    }

    @Test
    void testRejectWhenRootPathMissing() {

        FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "",
                        new String[]{"/content/site"},
                        new boolean[]{true},
                        new String[0]);

        mockNoRunningJobs();

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[]{"/content/site"});

        FullIndexTriggerResult result =
                service.triggerFullIndex(config);

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertEquals(
                "Root Path is required to start full indexing.",
                result.getMessage());
    }

    @Test
    void testRejectWhenRootPathDoesNotExist() throws Exception {

        mockNoRunningJobs();

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[]{"/content/site"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content"))
                .thenReturn(null);

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertTrue(result.getMessage().contains("Root path does not exist"));
    }

    @Test
    void testRejectWhenIncludeOutsideRoot() throws Exception {

        FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "/content",
                        new String[]{"/etc"},
                        new boolean[]{true},
                        new String[0]);

        mockNoRunningJobs();

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[]{"/etc"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content"))
                .thenReturn(resource);

        FullIndexTriggerResult result =
                service.triggerFullIndex(config);

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertEquals(
                "All include paths must be under root path.",
                result.getMessage());
    }

    @Test
    void testRejectWhenIncludeDoesNotExist() throws Exception {

        mockNoRunningJobs();

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[]{"/content/site"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content"))
                .thenReturn(resource);

        when(resolver.getResource("/content/site"))
                .thenReturn(null);

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertTrue(result.getMessage().contains("Include path does not exist"));
    }

    @Test
    void testRejectWhenExcludeOutsideRoot() throws Exception {

        FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "/content",
                        new String[]{"/content/site"},
                        new boolean[]{true},
                        new String[]{"/etc"});

        mockNoRunningJobs();

        mockValidPaths();

        FullIndexTriggerResult result =
                service.triggerFullIndex(config);

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertEquals(
                "Exclude paths must be under root or include paths.",
                result.getMessage());
    }

    @Test
    void testRejectWhenNoEffectiveIncludes() throws Exception {

        mockNoRunningJobs();

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[0]);

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content"))
                .thenReturn(resource);

        when(resolver.getResource("/content/site"))
                .thenReturn(resource);

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertEquals(
                "No valid include paths under the configured root path.",
                result.getMessage());
    }

    @Test
    void testCreateJobSuccessfully() throws Exception {

        mockNoRunningJobs();
        mockValidPaths();

        when(jobManager.addJob(
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                anyMap()))
                .thenReturn(job);

        when(job.getId())
                .thenReturn("job-123");

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertTrue(result.isAccepted());
        assertEquals(202, result.getHttpStatus());
        assertEquals("job-123", result.getJobId());
        assertEquals(
                "Full index started in background.",
                result.getMessage());

        verify(jobManager)
                .addJob(
                        eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                        anyMap());
        verify(executionService).prepareForQueuedJob();
    }

    @Test
    void testJobCreationFails() throws Exception {

        mockNoRunningJobs();
        mockValidPaths();

        when(jobManager.addJob(
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                anyMap()))
                .thenReturn(null);

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertEquals(
                "Could not queue full index job.",
                result.getMessage());
    }

    @Test
    void testResolverLoginException() throws Exception {

        mockNoRunningJobs();

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[]{"/content/site"});

        when(resolverUtil.getServiceResolver())
                .thenThrow(new LoginException("login failed"));

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(400, result.getHttpStatus());
        assertTrue(result.getMessage().contains("Root path does not exist"));
    }

    @Test
    void testRejectWhenQueuedJobExists() {

        when(jobManager.findJobs(
                eq(JobManager.QueryType.ACTIVE),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                ArgumentMatchers.<Map<String, Object>[]>any()))
                .thenReturn(Collections.emptyList());

        when(jobManager.findJobs(
                eq(JobManager.QueryType.QUEUED),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                ArgumentMatchers.<Map<String, Object>[]>any()))
                .thenReturn(Collections.singleton(job));

        FullIndexTriggerResult result =
                service.triggerFullIndex(createConfig());

        assertFalse(result.isAccepted());
        assertEquals(409, result.getHttpStatus());
        assertEquals(
                "A full index job is already running or queued.",
                result.getMessage());
    }

    @Test
    void testAllowExcludeInsideInclude() throws Exception {

        FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "/content",
                        new String[]{"/content/site"},
                        new boolean[]{true},
                        new String[]{"/content/site/excluded"});

        mockNoRunningJobs();
        mockValidPaths();

        when(jobManager.addJob(
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                anyMap()))
                .thenReturn(job);

        when(job.getId())
                .thenReturn("job-456");

        FullIndexTriggerResult result =
                service.triggerFullIndex(config);

        assertTrue(result.isAccepted());
        assertEquals(202, result.getHttpStatus());
    }

    @Test
    void testBlankIncludePathIgnored() throws Exception {

        FullIndexPathConfig config =
                new FullIndexPathConfig(
                        "/content",
                        new String[]{" "},
                        new boolean[]{true},
                        new String[0]);

        mockNoRunningJobs();

        when(pathConfigurationService.resolveEffectiveIncludes(any()))
                .thenReturn(new String[]{"/content"});

        when(resolverUtil.getServiceResolver())
                .thenReturn(resolver);

        when(resolver.getResource("/content"))
                .thenReturn(resource);

        when(jobManager.addJob(
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                anyMap()))
                .thenReturn(job);

        when(job.getId())
                .thenReturn("job-789");

        FullIndexTriggerResult result =
                service.triggerFullIndex(config);

        assertTrue(result.isAccepted());
        assertEquals(202, result.getHttpStatus());
    }
}