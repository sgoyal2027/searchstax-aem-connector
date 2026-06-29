package com.searchstax.aem.connector.core.config.wizard;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(AemContextExtension.class)
class OsgiBackedValueMapResourceTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Test
    void adaptToValueMap_returnsConfiguredProperties() {
        final ValueMap source = new ValueMapDecorator(Map.of("endpointUrl", "https://api.example.com"));
        final OsgiBackedValueMapResource resource = new OsgiBackedValueMapResource(
                context.resourceResolver(),
                "/apps/searchstaxconnector/wizard-bindings/api/jcr:content",
                "cq:PageContent",
                source);

        final ValueMap adapted = resource.adaptTo(ValueMap.class);

        assertNotNull(adapted);
        assertEquals("https://api.example.com", adapted.get("endpointUrl", String.class));
    }

    @Test
    void adaptToOtherType_delegatesToSuper() {
        final OsgiBackedValueMapResource resource = new OsgiBackedValueMapResource(
                context.resourceResolver(),
                "/apps/searchstaxconnector/wizard-bindings/api/jcr:content",
                "cq:PageContent",
                new ValueMapDecorator(Map.of()));

        assertNull(resource.adaptTo(String.class));
    }
}
