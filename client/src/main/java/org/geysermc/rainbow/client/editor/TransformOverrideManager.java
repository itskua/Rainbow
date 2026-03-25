package org.geysermc.rainbow.client.editor;

import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;
import org.geysermc.rainbow.mapping.animation.TransformOverrideRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class TransformOverrideManager {
    private static final Codec<Map<String, ItemTransformOverrides>> CODEC = Codec.unboundedMap(Codec.STRING, ItemTransformOverrides.CODEC);
    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve(Rainbow.MOD_ID).resolve("offsets.json");

    private TransformOverrideManager() {
    }

    public static void load() {
        RainbowIO.safeIO(() -> {
            if (!java.nio.file.Files.exists(PATH)) {
                TransformOverrideRegistry.replaceAll(Map.of());
                return null;
            }

            TransformOverrideRegistry.replaceAll(CodecUtil.tryReadJson(CODEC, PATH));
            return null;
        });
    }

    public static void save() {
        RainbowIO.safeIO(() -> {
            CodecUtil.trySaveJson(CODEC, TransformOverrideRegistry.snapshot(), PATH);
            return null;
        });
    }

    public static ItemTransformOverrides get(String identifier) {
        return TransformOverrideRegistry.get(identifier);
    }

    public static void put(String identifier, ItemTransformOverrides overrides) {
        TransformOverrideRegistry.put(identifier, overrides);
        save();
    }

    public static void reset(String identifier) {
        TransformOverrideRegistry.remove(identifier);
        save();
    }

    public static Optional<String> identifierForHeldItem() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Optional.empty();
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        if (model == null) {
            return Optional.empty();
        }
        return Optional.of(Rainbow.bedrockSafeIdentifier(model));
    }
}
