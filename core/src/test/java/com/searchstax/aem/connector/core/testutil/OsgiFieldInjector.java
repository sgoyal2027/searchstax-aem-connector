package com.searchstax.aem.connector.core.testutil;

import java.lang.reflect.Field;

public final class OsgiFieldInjector {

    private OsgiFieldInjector() {
    }

    public static void inject(final Object target, final String fieldName, final Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inject field " + fieldName, e);
        }
    }
}
