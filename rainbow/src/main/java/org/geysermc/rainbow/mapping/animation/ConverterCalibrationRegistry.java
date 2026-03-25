package org.geysermc.rainbow.mapping.animation;

public final class ConverterCalibrationRegistry {
    private static volatile ItemTransformOverrides calibration = ItemTransformOverrides.DEFAULT;

    private ConverterCalibrationRegistry() {
    }

    public static ItemTransformOverrides get() {
        return calibration;
    }

    public static void set(ItemTransformOverrides calibration) {
        ConverterCalibrationRegistry.calibration = calibration;
    }

    public static void reset() {
        calibration = ItemTransformOverrides.DEFAULT;
    }
}
