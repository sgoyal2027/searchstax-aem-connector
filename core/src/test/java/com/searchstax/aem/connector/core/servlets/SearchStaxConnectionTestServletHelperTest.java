package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchStaxConnectionTestServletHelperTest {

    @InjectMocks
    private SearchStaxConnectionTestServlet servlet;

    @Mock
    private CryptoSupport cryptoSupport;

    @Test
    void resolveSearchProbeUrl_appendsEmselectForGeneralEndpoint() throws Exception {
        assertEquals(
                "https://example.com/emselect?q=*&rows=1",
                invoke("resolveSearchProbeUrl", "https://example.com", "general"));
    }

    @Test
    void resolveSearchProbeUrl_keepsUpdateEndpointUnchanged() throws Exception {
        assertEquals(
                "https://example.com/update",
                invoke("resolveSearchProbeUrl", "https://example.com/update", "searchUpdate"));
    }

    @Test
    void resolveSearchProbeUrl_doesNotDuplicateEmselectSuffix() throws Exception {
        assertEquals(
                "https://example.com/emselect?q=*&rows=1",
                invoke("resolveSearchProbeUrl", "https://example.com/emselect", "searchSelect"));
    }

    @Test
    void mapFailureMessage_mapsAuthAndNotFoundStatuses() throws Exception {
        assertEquals("Invalid or unauthorized token.", invoke("mapFailureMessage", 401));
        assertEquals("Incorrect Host URL or context path.", invoke("mapFailureMessage", 404));
        assertTrue(invoke("mapFailureMessage", 500).toString().contains("HTTP status 500"));
    }

    @Test
    void mapSearchSuccessOrFailure_returnsSuccessMessage() throws Exception {
        assertEquals(
                "Connection successful.",
                invoke("mapSearchSuccessOrFailure", "searchSelect", true, 200));
    }

    @Test
    void mapSearchSuccessOrFailure_usesUpdateLabelForSearchUpdate() throws Exception {
        final String message = invoke("mapSearchSuccessOrFailure", "searchUpdate", false, 403).toString();

        assertTrue(message.contains("Update Endpoint"));
        assertTrue(message.contains("Invalid or unauthorized token"));
    }

    @Test
    void escapeJson_escapesQuotesAndBackslashes() throws Exception {
        assertEquals("line \\\"quoted\\\"", invoke("escapeJson", "line \"quoted\""));
    }

    @Test
    void unprotectTokenIfNeeded_decryptsProtectedValue() throws Exception {
        when(cryptoSupport.isProtected("{protected}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected}")).thenReturn("plain-token");

        assertEquals("plain-token", invoke("unprotectTokenIfNeeded", "{protected}"));
    }

    @Test
    void unprotectTokenIfNeeded_returnsRawValueWhenCryptoFails() throws Exception {
        when(cryptoSupport.isProtected("{protected}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected}")).thenThrow(new CryptoException("failed"));

        assertEquals("{protected}", invoke("unprotectTokenIfNeeded", "{protected}"));
    }

    private Object invoke(final String methodName, final Object... args) throws Exception {
        final Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
            if (args[i] instanceof Integer) {
                types[i] = int.class;
            } else if (args[i] instanceof Boolean) {
                types[i] = boolean.class;
            }
        }
        final Method method = SearchStaxConnectionTestServlet.class.getDeclaredMethod(methodName, types);
        method.setAccessible(true);
        return method.invoke(servlet, args);
    }
}
