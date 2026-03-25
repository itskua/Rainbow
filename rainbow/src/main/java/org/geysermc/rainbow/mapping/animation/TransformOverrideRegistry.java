package org.geysermc.rainbow.mapping.animation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TransformOverrideRegistry {
    private static final Map<String, ItemTransformOverrides> OVERRIDES = new ConcurrentHashMap<>();

    private TransformOverrideRegistry() {
    }

    public static ItemTransformOverrides get(String identifier) {
        return OVERRIDES.getOrDefault(identifier, ItemTransformOverrides.DEFAULT);
    }

    public static void put(String identifier, ItemTransformOverrides overrides) {
        OVERRIDES.put(identifier, overrides);
    }

    public static void remove(String identifier) {
        OVERRIDES.remove(identifier);
    }

    public static void replaceAll(Map<String, ItemTransformOverrides> overrides) {
        OVERRIDES.clear();
        OVERRIDES.putAll(overrides);
    }

    public static Map<String, ItemTransformOverrides> snapshot() {
        return Map.copyOf(OVERRIDES);
    }
}
