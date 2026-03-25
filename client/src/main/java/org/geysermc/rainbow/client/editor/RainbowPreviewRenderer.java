package org.geysermc.rainbow.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class RainbowPreviewRenderer {
    private RainbowPreviewRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, Minecraft minecraft, int width, int height) {
        RainbowOverlay overlay = RainbowOverlay.getInstance();
        if (!overlay.shouldRender(minecraft) || !overlay.isPreviewVisible() || minecraft.player == null) {
            return;
        }

        int panelWidth = Math.min(320, width / 3);
        int left = width - panelWidth - 24;
        int top = 24;
        int right = width - 24;
        int bottom = height - 24;
        overlay.setPreviewBounds(left, top, right, bottom);
        overlay.updatePreviewInteraction(minecraft);

        guiGraphics.fill(left, top, right, bottom, 0x6A08090C);
        guiGraphics.fill(left, top, right, top + 22, 0xAA1A2436);
        guiGraphics.drawString(minecraft.font, Component.literal("Rainbow Preview"), left + 8, top + 7, 0xFFFFFF, false);

        if (overlay.targetIdentifier() != null) {
            guiGraphics.drawString(minecraft.font, overlay.targetIdentifier(), left + 8, top + 30, 0xB7C2D0, false);
        }

        TransformEditorSession.PreviewMode mode = TransformEditorSession.previewMode();
        guiGraphics.drawString(minecraft.font, Component.literal(switch (mode) {
            case FIRST_PERSON -> "First Person Preview";
            case THIRD_PERSON -> "Third Person Preview";
            case HEAD -> "Head Preview";
        }), left + 8, top + 44, 0xE8EEF9, false);

        if (overlay.showCalibrationComparison()) {
            renderComparison(guiGraphics, minecraft, overlay, left, top, right, bottom, mode);
            return;
        }

        int renderLeft = left + 10;
        int renderTop = top + 64;
        int renderRight = right - 10;
        int renderBottom = bottom - 10;
        int scale = 62;
        float mouseX = (left + right) / 2.0F + overlay.previewYaw();
        float mouseY = top + 120.0F + overlay.previewPitch();

        if (mode == TransformEditorSession.PreviewMode.FIRST_PERSON) {
            scale = 98;
            renderLeft = left + 44;
            renderRight = right + 90;
            renderTop = top + 84;
            renderBottom = bottom - 24;
            mouseX = left + 230.0F;
            mouseY = top + 118.0F;
        } else if (mode == TransformEditorSession.PreviewMode.HEAD) {
            scale = 104;
            renderLeft = left + 18;
            renderRight = right - 18;
            renderTop = top + 78;
            renderBottom = bottom - 26;
            mouseX = (left + right) / 2.0F + overlay.previewYaw() * 0.65F;
            mouseY = top + 104.0F + overlay.previewPitch() * 0.65F;
        }

        TransformEditorSession.setPreviewRender(true);
        try {
            TransformEditorSession.setPreviewAdjustment(null);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                    renderLeft, renderTop, renderRight, renderBottom, scale,
                    0.0625F, mouseX, mouseY, minecraft.player);
        } finally {
            TransformEditorSession.setPreviewAdjustment(null);
            TransformEditorSession.setPreviewRender(false);
        }
    }

    private static void renderComparison(GuiGraphics guiGraphics, Minecraft minecraft, RainbowOverlay overlay, int left, int top, int right, int bottom,
                                         TransformEditorSession.PreviewMode mode) {
        int gap = 10;
        int bodyTop = top + 64;
        int bodyBottom = bottom - 12;
        int innerLeft = left + 10;
        int innerRight = right - 10;
        int availableWidth = innerRight - innerLeft;
        int panelWidth = (availableWidth - gap) / 2;

        int rawLeft = innerLeft;
        int rawRight = rawLeft + panelWidth;
        int calibratedLeft = rawRight + gap;
        int calibratedRight = innerRight;

        guiGraphics.fill(rawLeft, bodyTop, rawRight, bodyBottom, 0x44202833);
        guiGraphics.fill(calibratedLeft, bodyTop, calibratedRight, bodyBottom, 0x44202833);
        guiGraphics.drawString(minecraft.font, Component.literal("Raw Converter"), rawLeft + 8, bodyTop + 6, 0xFFFFFF, false);
        guiGraphics.drawString(minecraft.font, Component.literal("Calibrated"), calibratedLeft + 8, bodyTop + 6, 0xFFFFFF, false);

        renderEntityPanel(guiGraphics, minecraft, overlay, rawLeft, bodyTop + 20, rawRight, bodyBottom - 8, mode, overlay.previewRawAdjustment(), false);
        renderEntityPanel(guiGraphics, minecraft, overlay, calibratedLeft, bodyTop + 20, calibratedRight, bodyBottom - 8, mode, overlay.previewCalibratedAdjustment(), true);
    }

    private static void renderEntityPanel(GuiGraphics guiGraphics, Minecraft minecraft, RainbowOverlay overlay,
                                          int left, int top, int right, int bottom,
                                          TransformEditorSession.PreviewMode mode, org.geysermc.rainbow.mapping.animation.TransformAdjustment adjustment,
                                          boolean interactive) {
        int renderLeft = left;
        int renderTop = top;
        int renderRight = right;
        int renderBottom = bottom;
        int scale = 54;
        float mouseX = (left + right) / 2.0F + (interactive ? overlay.previewYaw() : 0.0F);
        float mouseY = top + 92.0F + (interactive ? overlay.previewPitch() : 0.0F);

        if (mode == TransformEditorSession.PreviewMode.FIRST_PERSON) {
            scale = 74;
            renderLeft = left + 18;
            renderRight = right + 48;
            renderTop = top + 10;
            renderBottom = bottom - 2;
            mouseX = left + 130.0F;
            mouseY = top + 70.0F;
        } else if (mode == TransformEditorSession.PreviewMode.HEAD) {
            scale = 78;
            renderLeft = left + 8;
            renderRight = right - 8;
            renderTop = top + 8;
            renderBottom = bottom - 2;
            mouseX = (left + right) / 2.0F + (interactive ? overlay.previewYaw() * 0.65F : 0.0F);
            mouseY = top + 78.0F + (interactive ? overlay.previewPitch() * 0.65F : 0.0F);
        }

        TransformEditorSession.setPreviewRender(true);
        try {
            TransformEditorSession.setPreviewAdjustment(adjustment);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                    renderLeft, renderTop, renderRight, renderBottom, scale,
                    0.0625F, mouseX, mouseY, minecraft.player);
        } finally {
            TransformEditorSession.setPreviewAdjustment(null);
            TransformEditorSession.setPreviewRender(false);
        }
    }
}
