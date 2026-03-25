package org.geysermc.rainbow.client.editor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;
import org.geysermc.rainbow.mapping.animation.TransformAdjustment;

public final class TransformEditorSession {
    public enum PreviewMode {
        FIRST_PERSON,
        THIRD_PERSON,
        HEAD
    }

    private static boolean active;
    private static boolean previewRender;
    private static String targetIdentifier;
    private static ItemTransformOverrides overrides = ItemTransformOverrides.DEFAULT;
    private static PreviewMode previewMode = PreviewMode.THIRD_PERSON;

    private TransformEditorSession() {
    }

    public static void open(String identifier, ItemTransformOverrides overrides) {
        active = true;
        targetIdentifier = identifier;
        TransformEditorSession.overrides = overrides;
    }

    public static void close() {
        active = false;
        previewRender = false;
        targetIdentifier = null;
        overrides = ItemTransformOverrides.DEFAULT;
        previewMode = PreviewMode.THIRD_PERSON;
    }

    public static boolean isActive() {
        return active;
    }

    public static void setOverrides(ItemTransformOverrides overrides) {
        TransformEditorSession.overrides = overrides;
    }

    public static void setPreviewMode(PreviewMode previewMode) {
        TransformEditorSession.previewMode = previewMode;
    }

    public static PreviewMode previewMode() {
        return previewMode;
    }

    public static boolean isPreviewRender() {
        return previewRender;
    }

    public static void setPreviewRender(boolean previewRender) {
        TransformEditorSession.previewRender = previewRender;
    }

    public static TransformAdjustment activeAdjustment() {
        return switch (previewMode) {
            case FIRST_PERSON -> overrides.firstPerson();
            case THIRD_PERSON -> overrides.thirdPerson();
            case HEAD -> overrides.head();
        };
    }

    public static boolean matches(ItemStack stack) {
        if (!active || stack.isEmpty()) {
            return false;
        }

        var identifier = stack.get(DataComponents.ITEM_MODEL);
        return identifier != null && Rainbow.bedrockSafeIdentifier(identifier).equals(targetIdentifier);
    }
}
