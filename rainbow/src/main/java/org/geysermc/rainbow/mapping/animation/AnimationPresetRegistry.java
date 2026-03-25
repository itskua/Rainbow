package org.geysermc.rainbow.mapping.animation;

public final class AnimationPresetRegistry {
    private static volatile boolean largeItemPresetEnabled;
    private static volatile ItemTransformOverrides regularPreset = ItemTransformOverrides.DEFAULT;
    private static volatile ItemTransformOverrides handheldPreset = ItemTransformOverrides.DEFAULT;

    private AnimationPresetRegistry() {
    }

    public static boolean largeItemPresetEnabled() {
        return largeItemPresetEnabled;
    }

    public static void setLargeItemPresetEnabled(boolean enabled) {
        largeItemPresetEnabled = enabled;
    }

    public static ItemTransformOverrides regularPreset() {
        return regularPreset;
    }

    public static void setRegularPreset(ItemTransformOverrides preset) {
        regularPreset = preset;
    }

    public static ItemTransformOverrides handheldPreset() {
        return handheldPreset;
    }

    public static void setHandheldPreset(ItemTransformOverrides preset) {
        handheldPreset = preset;
    }
}
