package org.geysermc.rainbow.client.editor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.animation.AnimationPresetRegistry;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;

import java.nio.file.Path;

public final class AnimationPresetManager {
    private static final ItemTransformOverrides DEFAULT_REGULAR_PRESET = new ItemTransformOverrides(
            new org.geysermc.rainbow.mapping.animation.TransformAdjustment(
                    new org.joml.Vector3f(-5.02343F, 13.98217F, -3.72306F),
                    new org.joml.Vector3f(271.13165F, -26.19883F, -69.62689F),
                    new org.joml.Vector3f(1.4F, 1.4F, 1.4F)
            ),
            new org.geysermc.rainbow.mapping.animation.TransformAdjustment(
                    new org.joml.Vector3f(1.29869F, 28.09352F, -7.93738F),
                    new org.joml.Vector3f(283.00481F, -107.53494F, -439.9079F),
                    new org.joml.Vector3f(0.55F, 0.55F, 0.55F)
            ),
            ItemTransformOverrides.DEFAULT.head()
    );
    private static final ItemTransformOverrides DEFAULT_HANDHELD_PRESET = new ItemTransformOverrides(
            new org.geysermc.rainbow.mapping.animation.TransformAdjustment(
                    new org.joml.Vector3f(2.10302F, -3.83105F, 10.43465F),
                    new org.joml.Vector3f(304.15889F, -147.41223F, -238.55658F),
                    new org.joml.Vector3f(1.4F, 1.4F, 1.4F)
            ),
            new org.geysermc.rainbow.mapping.animation.TransformAdjustment(
                    new org.joml.Vector3f(-3.36827F, 26.60414F, -14.20191F),
                    new org.joml.Vector3f(-36.67548F, -86.85374F, -179.50132F),
                    new org.joml.Vector3f(0.75F, 1.0F, 1.0F)
            ),
            ItemTransformOverrides.DEFAULT.head()
    );

    private record Settings(boolean largeItemPresetEnabled, ItemTransformOverrides regularPreset, ItemTransformOverrides handheldPreset) {
        private static final Settings DEFAULT = new Settings(false, DEFAULT_REGULAR_PRESET, DEFAULT_HANDHELD_PRESET);
        private static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.optionalFieldOf("large_item_preset_enabled", false).forGetter(Settings::largeItemPresetEnabled),
                        ItemTransformOverrides.CODEC.optionalFieldOf("regular_preset", DEFAULT_REGULAR_PRESET).forGetter(Settings::regularPreset),
                        ItemTransformOverrides.CODEC.optionalFieldOf("handheld_preset", DEFAULT_HANDHELD_PRESET).forGetter(Settings::handheldPreset)
                ).apply(instance, Settings::new)
        );
    }

    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve(Rainbow.MOD_ID).resolve("animation-presets.json");

    private AnimationPresetManager() {
    }

    public static void load() {
        RainbowIO.safeIO(() -> {
            if (!java.nio.file.Files.exists(PATH)) {
                apply(Settings.DEFAULT);
                save();
                return null;
            }

            Settings settings = CodecUtil.tryReadJson(Settings.CODEC, PATH);
            apply(settings);
            return null;
        });
    }

    public static void save() {
        RainbowIO.safeIO(() -> {
            CodecUtil.trySaveJson(Settings.CODEC, new Settings(
                    AnimationPresetRegistry.largeItemPresetEnabled(),
                    AnimationPresetRegistry.regularPreset(),
                    AnimationPresetRegistry.handheldPreset()
            ), PATH);
            return null;
        });
    }

    public static boolean largeItemPresetEnabled() {
        return AnimationPresetRegistry.largeItemPresetEnabled();
    }

    public static void setLargeItemPresetEnabled(boolean enabled) {
        AnimationPresetRegistry.setLargeItemPresetEnabled(enabled);
        save();
    }

    public static Path path() {
        return PATH;
    }

    private static void apply(Settings settings) {
        AnimationPresetRegistry.setLargeItemPresetEnabled(settings.largeItemPresetEnabled());
        AnimationPresetRegistry.setRegularPreset(settings.regularPreset());
        AnimationPresetRegistry.setHandheldPreset(settings.handheldPreset());
    }
}
