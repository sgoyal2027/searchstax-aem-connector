package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component(service = EmailConfigService.class)
public class EmailConfigServiceImpl implements EmailConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConfigServiceImpl.class);

    public static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/emailconfig";

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public EmailConfig getConfiguration() {
        final EmailConfig config = new EmailConfig();
        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource resource = resolver.getResource(CONFIG_PATH);
            if (resource == null) {
                LOG.warn("Email configuration not found at {}", CONFIG_PATH);
                return config;
            }
            final ValueMap vm = resource.getValueMap();
            config.setSmtpHost(vm.get("smtpHost", ""));
            config.setSmtpPort(vm.get("smtpPort", 25));
            config.setSmtpUser(vm.get("smtpUser", ""));
            config.setSmtpPassword(vm.get("smtpPassword", ""));
            config.setFromEmail(vm.get("fromEmail", ""));
            config.setReceiverEmails(vm.get("receiverEmails", ""));
            config.setSmtpUseSsl(vm.get("smtpUseSSL", false));
            config.setSmtpUseStartTls(vm.get("smtpUseStartTLS", false));
            config.setSmtpRequireStartTls(vm.get("smtpRequireStartTLS", false));
            config.setDebugEmail(vm.get("debugEmail", false));
            config.setOauthFlow(vm.get("oauthFlow", false));
            config.setNotifyOnIndexingFailure(vm.get("notifyOnIndexingFailure", true));
        } catch (LoginException e) {
            LOG.error("Unable to load email configuration", e);
        }
        return config;
    }

    @Override
    public String[] getReceiverAddresses() {
        final EmailConfig config = getConfiguration();
        if (!config.isNotifyOnIndexingFailure()) {
            return new String[0];
        }
        return parseReceiverEmails(config.getReceiverEmails());
    }

    public static String[] parseReceiverEmails(final String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new String[0];
        }
        final String[] parts = raw.split(",");
        final List<String> addresses = new ArrayList<>();
        for (final String part : parts) {
            final String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                addresses.add(trimmed);
            }
        }
        return addresses.toArray(new String[0]);
    }
}
