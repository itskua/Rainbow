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
        if (!overlay.shouldRender(minecraft) || minecraft.player == null) {
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
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                    renderLeft, renderTop, renderRight, renderBottom, scale,
                    0.0625F, mouseX, mouseY, minecraft.player);
        } finally {
            TransformEditorSession.setPreviewRender(false);
        }
    }
}
