package org.geysermc.rainbow.client.editor;

import net.fabricmc.loader.api.FabricLoader;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.animation.ConverterCalibrationRegistry;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;

import java.nio.file.Path;

public final class ConverterCalibrationManager {
    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve(Rainbow.MOD_ID).resolve("converter-calibration.json");

    private ConverterCalibrationManager() {
    }

    public static void load() {
        RainbowIO.safeIO(() -> {
            if (!java.nio.file.Files.exists(PATH)) {
                ConverterCalibrationRegistry.reset();
                return null;
            }

            ConverterCalibrationRegistry.set(CodecUtil.tryReadJson(ItemTransformOverrides.CODEC, PATH));
            return null;
        });
    }

    public static void save() {
        RainbowIO.safeIO(() -> {
            CodecUtil.trySaveJson(ItemTransformOverrides.CODEC, ConverterCalibrationRegistry.get(), PATH);
            return null;
        });
    }

    public static ItemTransformOverrides get() {
        return ConverterCalibrationRegistry.get();
    }

    public static void put(ItemTransformOverrides calibration) {
        ConverterCalibrationRegistry.set(calibration);
        save();
    }

    public static void reset() {
        ConverterCalibrationRegistry.reset();
        save();
    }
}
