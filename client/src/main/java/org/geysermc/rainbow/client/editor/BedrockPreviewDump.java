package org.geysermc.rainbow.client.editor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;

import java.nio.file.Path;

public record BedrockPreviewDump(String identifier, ItemTransformOverrides rawBase, ItemTransformOverrides base, ItemTransformOverrides override, ItemTransformOverrides finalValue) {
    public static final Codec<BedrockPreviewDump> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("identifier").forGetter(BedrockPreviewDump::identifier),
                    ItemTransformOverrides.CODEC.fieldOf("raw_base").forGetter(BedrockPreviewDump::rawBase),
                    ItemTransformOverrides.CODEC.fieldOf("base").forGetter(BedrockPreviewDump::base),
                    ItemTransformOverrides.CODEC.fieldOf("override").forGetter(BedrockPreviewDump::override),
                    ItemTransformOverrides.CODEC.fieldOf("final").forGetter(BedrockPreviewDump::finalValue)
            ).apply(instance, BedrockPreviewDump::new)
    );

    public static Path save(BedrockPreviewDump dump) {
        Path path = FabricLoader.getInstance().getGameDir()
                .resolve(Rainbow.MOD_ID)
                .resolve("preview-dumps")
                .resolve(dump.identifier() + ".json");
        RainbowIO.safeIO(() -> {
            CodecUtil.trySaveJson(CODEC, dump, path);
            return null;
        });
        return path;
    }
}
