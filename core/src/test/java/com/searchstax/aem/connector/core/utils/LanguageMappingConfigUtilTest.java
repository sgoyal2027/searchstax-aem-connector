package com.searchstax.aem.connector.core.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(AemContextExtension.class)
class LanguageMappingConfigUtilTest {

    private static final Gson GSON = new Gson();

    private final AemContext context = AppAemContext.newAemContext();

    @Test
    void loadOrPersistDefaultMappingsJson_returnsStoredMappings() throws Exception {
        final String stored = "[{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"english\"}]";
        context.create().resource(
                LanguageMappingConfigUtil.CONFIG_PATH,
                LanguageMappingConfigUtil.PROPERTY_NAME,
                stored);

        final String result =
                LanguageMappingConfigUtil.loadOrPersistDefaultMappingsJson(context.resourceResolver());

        assertEquals(stored, result);
    }

    @Test
    void loadOrPersistDefaultMappingsJson_persistsDefaultsWhenEmpty() throws Exception {
        context.create().resource(
                LanguageMappingConfigUtil.CONFIG_PATH,
                LanguageMappingConfigUtil.PROPERTY_NAME,
                "[]");

        final String result =
                LanguageMappingConfigUtil.loadOrPersistDefaultMappingsJson(context.resourceResolver());

        final List<LanguageMappingConfig> mappings =
                GSON.fromJson(result, new TypeToken<List<LanguageMappingConfig>>() {}.getType());
        assertFalse(mappings.isEmpty());
        assertEquals(
                GSON.toJson(LanguageMappingConfig.defaultMappings()),
                context.resourceResolver()
                        .getResource(LanguageMappingConfigUtil.CONFIG_PATH)
                        .getValueMap()
                        .get(LanguageMappingConfigUtil.PROPERTY_NAME, String.class));
    }
}
