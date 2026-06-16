package com.searchstax.aem.connector.core.config;

import com.searchstax.aem.connector.core.config.model.EmailConfig;

public interface EmailConfigService {

    EmailConfig getConfiguration();

    String[] getReceiverAddresses();
}
