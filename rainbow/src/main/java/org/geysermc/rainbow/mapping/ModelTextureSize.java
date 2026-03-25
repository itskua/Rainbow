package org.geysermc.rainbow.mapping;

public record ModelTextureSize(int width, int height) {
    public static final ModelTextureSize DEFAULT = new ModelTextureSize(16, 16);
}
