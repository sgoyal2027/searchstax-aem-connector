package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.Map;

@Component(service = ResolverUtil.class)
public class ResolverUtil {

    private static final String SUB_SERVICE_NAME = "searchstaxService";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public ResourceResolver getServiceResolver() throws LoginException {

        Map<String, Object> params = new HashMap<>();

        params.put(ResourceResolverFactory.SUBSERVICE, SUB_SERVICE_NAME);

        return resourceResolverFactory.getServiceResourceResolver(params);

    }
}
