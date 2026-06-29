package com.searchstax.aem.connector.core.services.impl;

import com.day.cq.dam.api.Asset;
import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingHelperServiceImplTest {

    @InjectMocks
    private IndexingHelperServiceImpl helper;

    @Mock
    private MetadataFieldConfigService metadataFieldConfigService;

    @Mock
    private InitialSetupConfigService initialSetupConfigService;

    @Test
    void cleanText_removesHtmlTagsAndNormalizesWhitespace() {
        assertEquals("Hello World", helper.cleanText("<p>Hello   World</p>"));
    }

    @Test
    void cleanText_replacesNbspWithSpace() {
        assertEquals("foo bar", helper.cleanText("foo&nbsp;bar"));
    }

    @Test
    void cleanText_returnsEmptyStringForNull() {
        assertEquals("", helper.cleanText(null));
    }

    @Test
    void shouldRetry_returnsFalseFor401And403() {
        assertFalse(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(401, "")));
        assertFalse(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(403, "")));
    }

    @Test
    void shouldRetry_returnsTrueFor429UnlessPlanLimitExceeded() {
        assertTrue(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(429, "Too Many Requests")));
        assertFalse(helper.shouldRetry(new com.searchstax.aem.connector.core.dto.response.ApiResponse(429, "Plan Limit Exceeded")));
    }

    @Test
    void isPermanentFailure_includes413() {
        assertTrue(helper.isPermanentFailure(new com.searchstax.aem.connector.core.dto.response.ApiResponse(413, "")));
    }

    @Test
    void formatFailureMessage_permanentFailure_includesHttpStatusAndBody() {
        final String message = helper.formatFailureMessage(
                "PERMANENT_FAILURE",
                new com.searchstax.aem.connector.core.dto.response.ApiResponse(
                        401, "{\"error\":\"invalid token\"}"),
                null);

        assertTrue(message.contains("HTTP 401 Unauthorized"));
        assertTrue(message.contains("invalid token"));
        assertTrue(message.contains("will not be retried"));
    }

    @Test
    void formatFailureMessage_planLimitExceeded_isReadable() {
        final String message = helper.formatFailureMessage(
                "PLAN_LIMIT_EXCEEDED",
                new com.searchstax.aem.connector.core.dto.response.ApiResponse(
                        429, "Plan Limit Exceeded"),
                null);

        assertTrue(message.contains("plan document limit exceeded"));
        assertTrue(message.contains("HTTP 429"));
    }

    @Test
    void formatFailureMessage_maxRetryCountExhausted_includesRetryLimit() {
        final String message = helper.formatFailureMessage(
                "MAX_RETRY_COUNT_EXHAUSTED",
                new com.searchstax.aem.connector.core.dto.response.ApiResponse(503, "Service Unavailable"),
                null);

        assertTrue(message.contains("5 retry attempts"));
        assertTrue(message.contains("HTTP 503"));
    }

    @Test
    void formatFailureMessage_deletePermanentFailure_isReadable() {
        final String message = helper.formatFailureMessage(
                "DELETE_PERMANENT_FAILURE",
                new com.searchstax.aem.connector.core.dto.response.ApiResponse(404, "not found"),
                null);

        assertTrue(message.contains("delete request"));
        assertTrue(message.contains("will not be retried"));
        assertTrue(message.contains("HTTP 404"));
    }

    @Test
    void formatFailureMessage_buildFailure_includesExceptionDetail() {
        final String message = helper.formatFailureMessage(
                "MAX_RETRY_COUNT_REACHED",
                null,
                new IllegalStateException("document build failed"));

        assertTrue(message.contains("Could not build the index document"));
        assertTrue(message.contains("document build failed"));
    }

    @Test
    void isSuccess_returnsFalseForNullOrNon2xx() {
        assertFalse(helper.isSuccess(null));
        assertFalse(helper.isSuccess(new ApiResponse(500, "")));
        assertTrue(helper.isSuccess(new ApiResponse(200, "{\"success\":true}")));
        assertFalse(helper.isSuccess(new ApiResponse(200, "{\"success\":false}")));
    }

    @Test
    void calculateDelay_usesExponentialBackoff() {
        assertEquals(700L, helper.calculateDelay(1));
        assertEquals(1400L, helper.calculateDelay(2));
        assertEquals(2800L, helper.calculateDelay(3));
    }

    @Test
    void shouldRetry_returnsTrueFor503() {
        assertTrue(helper.shouldRetry(new ApiResponse(503, "unavailable")));
    }

    @Test
    void resolveFieldName_appendsLanguageSuffixForTextFields() {
        assertEquals("title_txt_en", helper.resolveFieldName("title", "text", "value", "en"));
        assertEquals("tags_ss", helper.resolveFieldName("tags", null, new String[]{"a"}, "en"));
        assertEquals("published_dt", helper.resolveFieldName("published", null, new GregorianCalendar(), "en"));
    }

    @Test
    void normalizeMetadataValue_convertsCalendarToInstantString() {
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(2026, Calendar.JUNE, 29, 12, 0, 0);

        final Object normalized = helper.normalizeMetadataValue(calendar);

        assertTrue(normalized.toString().contains("2026"));
    }

    @Test
    void addConfiguredMetadataFields_mapsEnabledProperties() {
        final MetadataFieldMappingConfig mapping = new MetadataFieldMappingConfig();
        mapping.setEnabled(true);
        mapping.setAemField("jcr:title");
        mapping.setSearchStaxField("title_txt_en");
        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(List.of(mapping));

        final ValueMap valueMap = mock(ValueMap.class);
        when(valueMap.get("jcr:title")).thenReturn("Page title");

        final Map<String, Object> document = new HashMap<>();
        helper.addConfiguredMetadataFields(document, valueMap);

        assertEquals("Page title", document.get("title_txt_en"));
    }

    @Test
    void isSupportedAsset_honorsAllowedFileExtensions() {
        final InitialSetupConfig config = new InitialSetupConfig();
        config.setAllowedFiles(new String[]{"pdf", "jpg"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        final Asset pdfAsset = mock(Asset.class);
        when(pdfAsset.getName()).thenReturn("brochure.pdf");

        final Asset txtAsset = mock(Asset.class);
        when(txtAsset.getName()).thenReturn("notes.txt");

        assertTrue(helper.isSupportedAsset(pdfAsset));
        assertFalse(helper.isSupportedAsset(txtAsset));
    }

    @Test
    void isSuccess_returnsFalseWhenBodyContainsSuccessFalse() {
        assertFalse(helper.isSuccess(new ApiResponse(200, "{\"success\":false,\"message\":\"rejected\"}")));
    }

    @Test
    void shouldRetry_returnsTrueForNullResponse() {
        assertTrue(helper.shouldRetry(null));
    }

    @Test
    void isPlanLimitExceeded_detects429WithPlanLimitMessage() {
        final ApiResponse response = new ApiResponse(429, "Plan Limit Exceeded for tenant");

        assertTrue(helper.isPlanLimitExceeded(response));
        assertFalse(helper.shouldRetry(response));
    }

    @Test
    void addField_skipsNullValues() {
        final Map<String, Object> document = new HashMap<>();
        helper.addField(document, "title", null);
        helper.addField(document, "title", "Hello");

        assertEquals(1, document.size());
        assertEquals("Hello", document.get("title"));
    }

    @Test
    void detectLanguage_returnsEnglishDefault() {
        assertEquals("en", helper.detectLanguage("Bonjour le monde"));
        assertEquals("en", helper.detectLanguage(null));
    }

    @Test
    void isSupportedAsset_returnsTrueWhenAllowedFilesNotConfigured() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(new InitialSetupConfig());

        assertTrue(helper.isSupportedAsset(mock(Asset.class)));
    }

    @Test
    void isSupportedAsset_returnsFalseForExtensionlessName() {
        final InitialSetupConfig config = new InitialSetupConfig();
        config.setAllowedFiles(new String[]{"pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        final Asset asset = mock(Asset.class);
        when(asset.getName()).thenReturn("README");

        assertFalse(helper.isSupportedAsset(asset));
    }

    @Test
    void extractText_returnsEmptyWhenOriginalRenditionMissing() {
        final Asset asset = mock(Asset.class);
        when(asset.getPath()).thenReturn("/content/dam/file.pdf");
        when(asset.getOriginal()).thenReturn(null);

        assertEquals("", helper.extractText(asset));
    }
}
