package com.searchstax.aem.connector.core.utils;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtectedValueCodecTest {

    @Mock
    private CryptoSupport cryptoSupport;

    private ProtectedValueCodec codec;

    @BeforeEach
    void setUp() {
        codec = new ProtectedValueCodec();
        org.mockito.Mockito.lenient().when(cryptoSupport.isProtected(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
    }

    @Test
    void unprotectIfNeeded_returnsPlainValueWhenNotProtected() throws CryptoException {
        injectCryptoSupport();
        when(cryptoSupport.isProtected("plain-token")).thenReturn(false);

        assertEquals("plain-token", codec.unprotectIfNeeded("plain-token"));
    }

    @Test
    void unprotectIfNeeded_decryptsProtectedValue() throws CryptoException {
        injectCryptoSupport();
        when(cryptoSupport.isProtected("{protected}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected}")).thenReturn("decrypted");

        assertEquals("decrypted", codec.unprotectIfNeeded("{protected}"));
    }

    @Test
    void looksEncrypted_detectsProtectedBlobShape() throws CryptoException {
        injectCryptoSupport();
        final String blob = "{" + "x".repeat(50) + "}";

        when(cryptoSupport.isProtected(blob)).thenReturn(false);

        assertTrue(codec.looksEncrypted(blob));
        assertFalse(codec.looksEncrypted("short"));
    }

    private void injectCryptoSupport() {
        try {
            final var field = ProtectedValueCodec.class.getDeclaredField("cryptoSupport");
            field.setAccessible(true);
            field.set(codec, cryptoSupport);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
