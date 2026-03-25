package org.geysermc.rainbow.client.editor;

import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.MinecraftAssetResolver;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
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

    public static Optional<ItemTransformOverrides> finalTransformsForHeldItem() {
        return baseTransformsForHeldItem().map(base -> AnimationMapper.applyOverrides(
                base,
                get(identifierForHeldItem().orElseThrow())
        ));
    }

    public static Optional<ItemTransformOverrides> baseTransformsForHeldItem() {
        return resolveTransformsForHeldItem(true);
    }

    public static Optional<ItemTransformOverrides> rawBaseTransformsForHeldItem() {
        return resolveTransformsForHeldItem(false);
    }

    private static Optional<ItemTransformOverrides> resolveTransformsForHeldItem(boolean includeCalibration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Optional.empty();
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Identifier itemModelId = stack.get(DataComponents.ITEM_MODEL);
        if (itemModelId == null) {
            return Optional.empty();
        }

        MinecraftAssetResolver resolver = new MinecraftAssetResolver(minecraft);
        Optional<ClientItem> clientItem = resolver.getClientItem(itemModelId);
        if (clientItem.isEmpty()) {
            return Optional.empty();
        }

        ItemModel.Unbaked unbaked = clientItem.get().model();
        if (!(unbaked instanceof BlockModelWrapper.Unbaked wrapper)) {
            return Optional.empty();
        }

        return resolver.getResolvedModel(wrapper.model())
                .map(model -> includeCalibration
                        ? AnimationMapper.baseTransforms(model.getTopTransforms(), BedrockGeometryContext.isHandheldModel(model))
                        : AnimationMapper.rawBaseTransforms(model.getTopTransforms()));
    }
}
