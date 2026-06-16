package com.searchstax.aem.connector.core.utils;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decrypts values persisted with AEM {@link CryptoSupport#protect(String)}.
 */
@Component(service = ProtectedValueCodec.class)
public class ProtectedValueCodec {

    private static final Logger LOG = LoggerFactory.getLogger(ProtectedValueCodec.class);

    @Reference
    private CryptoSupport cryptoSupport;

    public String unprotectIfNeeded(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        if (cryptoSupport == null) {
            LOG.warn("CryptoSupport is unavailable; encrypted configuration values cannot be decrypted");
            return value.trim();
        }

        final String trimmed = value.trim();

        try {
            if (cryptoSupport.isProtected(trimmed)) {
                return cryptoSupport.unprotect(trimmed);
            }

            if (looksLikeProtectedBlob(trimmed)) {
                LOG.info("Attempting CryptoSupport unprotect for AEM protected-value blob");
                return cryptoSupport.unprotect(trimmed);
            }
        } catch (CryptoException e) {
            LOG.error("Failed to decrypt protected configuration value", e);
        }

        return trimmed;
    }

    public boolean looksEncrypted(final String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        final String trimmed = value.trim();
        if (cryptoSupport != null && cryptoSupport.isProtected(trimmed)) {
            return true;
        }

        return looksLikeProtectedBlob(trimmed);
    }

    private boolean looksLikeProtectedBlob(final String value) {
        return value.startsWith("{") && value.endsWith("}") && value.length() > 40;
    }
}
