package org.geysermc.rainbow.client.editor;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.client.imgui.RenderInterface;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;
import org.geysermc.rainbow.mapping.animation.TransformAdjustment;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class TransformEditorScreen extends Screen implements RenderInterface {
    private enum ViewMode {
        FIRST_PERSON("First Person", TransformEditorSession.PreviewMode.FIRST_PERSON),
        THIRD_PERSON("Third Person", TransformEditorSession.PreviewMode.THIRD_PERSON),
        HEAD("Head", TransformEditorSession.PreviewMode.HEAD);

        private final String label;
        private final TransformEditorSession.PreviewMode previewMode;

        ViewMode(String label, TransformEditorSession.PreviewMode previewMode) {
            this.label = label;
            this.previewMode = previewMode;
        }
    }

    private final Screen parent;
    private final String targetIdentifier;
    private ViewMode mode = ViewMode.THIRD_PERSON;
    private ItemTransformOverrides overrides;

    public TransformEditorScreen(Screen parent, String targetIdentifier) {
        super(Component.literal("Rainbow Transform Editor"));
        this.parent = parent;
        this.targetIdentifier = targetIdentifier;
        this.overrides = TransformOverrideManager.get(targetIdentifier);
        TransformEditorSession.open(targetIdentifier, overrides);
        TransformEditorSession.setPreviewMode(mode.previewMode);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        TransformEditorSession.close();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        guiGraphics.fill(16, 16, width - 16, height - 16, 0x66000000);
        guiGraphics.fill(width - 310, 16, width - 16, height - 16, 0x55000000);
        guiGraphics.drawCenteredString(font, Component.literal("Rainbow Preview"), width - 163, 24, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.literal(targetIdentifier), width - 163, 38, 0xB0B0B0);

        if (minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, width - 300, 56, 268, height - 120, 52,
                    0.0625F, width - 170.0F, 120.0F, minecraft.player);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderImGui(ImGuiIO io) {
        ImGui.setNextWindowPos(24, 24, ImGuiCond.Always);
        ImGui.setNextWindowSize(360, Math.max(360, height - 48), ImGuiCond.Always);

        if (ImGui.begin("Rainbow Item Editor")) {
            ImGui.text(targetIdentifier);
            ImGui.separator();

            if (ImGui.button("First Person")) {
                switchMode(ViewMode.FIRST_PERSON);
            }
            ImGui.sameLine();
            if (ImGui.button("Third Person")) {
                switchMode(ViewMode.THIRD_PERSON);
            }
            ImGui.sameLine();
            if (ImGui.button("Head")) {
                switchMode(ViewMode.HEAD);
            }

            ImGui.separator();
            ImGui.text("Editing: " + mode.label);

            renderVectorEditor("Position", "position", 0.5F);
            renderVectorEditor("Rotation", "rotation", 2.5F);
            renderVectorEditor("Scale", "scale", 0.05F);

            ImGui.separator();
            if (ImGui.button("Save")) {
                TransformOverrideManager.put(targetIdentifier, overrides);
            }
            ImGui.sameLine();
            if (ImGui.button("Reset")) {
                overrides = ItemTransformOverrides.DEFAULT;
                TransformEditorSession.setOverrides(overrides);
                TransformOverrideManager.reset(targetIdentifier);
            }
            ImGui.sameLine();
            if (ImGui.button("Close")) {
                onClose();
            }

            ImGui.separator();
            ImGui.textWrapped("Changes update the preview live. Save when the held item looks right, then export the pack.");
        }
        ImGui.end();
    }

    private void renderVectorEditor(String title, String group, float step) {
        ImGui.text(title);
        renderAxisRow("X", group, 0, step);
        renderAxisRow("Y", group, 1, step);
        renderAxisRow("Z", group, 2, step);
        ImGui.spacing();
    }

    private void renderAxisRow(String label, String group, int axis, float step) {
        float value = currentValue(group, axis);
        ImGui.text(label);
        ImGui.sameLine();
        if (ImGui.button("-##" + group + axis)) {
            adjust(group, axis, -step);
        }
        ImGui.sameLine();
        ImGui.text(String.format("%.2f", value));
        ImGui.sameLine();
        if (ImGui.button("+##" + group + axis)) {
            adjust(group, axis, step);
        }
    }

    private void switchMode(ViewMode mode) {
        this.mode = mode;
        TransformEditorSession.setPreviewMode(mode.previewMode);
    }

    private void adjust(String group, int axis, float amount) {
        TransformAdjustment active = activeAdjustment();
        Vector3f position = new Vector3f(active.position());
        Vector3f rotation = new Vector3f(active.rotation());
        Vector3f scale = new Vector3f(active.scale());

        Vector3f target = switch (group) {
            case "position" -> position;
            case "rotation" -> rotation;
            case "scale" -> scale;
            default -> throw new IllegalArgumentException("Unknown transform group " + group);
        };
        setAxis(target, axis, getAxis(target, axis) + amount);

        TransformAdjustment updated = new TransformAdjustment(position, rotation, scale);
        overrides = switch (mode) {
            case FIRST_PERSON -> new ItemTransformOverrides(updated, overrides.thirdPerson(), overrides.head());
            case THIRD_PERSON -> new ItemTransformOverrides(overrides.firstPerson(), updated, overrides.head());
            case HEAD -> new ItemTransformOverrides(overrides.firstPerson(), overrides.thirdPerson(), updated);
        };
        TransformEditorSession.setOverrides(overrides);
    }

    private float currentValue(String group, int axis) {
        TransformAdjustment adjustment = activeAdjustment();
        Vector3fc vector = switch (group) {
            case "position" -> adjustment.position();
            case "rotation" -> adjustment.rotation();
            case "scale" -> adjustment.scale();
            default -> throw new IllegalArgumentException("Unknown transform group " + group);
        };
        return getAxis(vector, axis);
    }

    private TransformAdjustment activeAdjustment() {
        return switch (mode) {
            case FIRST_PERSON -> overrides.firstPerson();
            case THIRD_PERSON -> overrides.thirdPerson();
            case HEAD -> overrides.head();
        };
    }

    private static float getAxis(Vector3fc vector, int axis) {
        return switch (axis) {
            case 0 -> vector.x();
            case 1 -> vector.y();
            case 2 -> vector.z();
            default -> throw new IllegalArgumentException("Unknown axis " + axis);
        };
    }

    private static void setAxis(Vector3f vector, int axis, float value) {
        switch (axis) {
            case 0 -> vector.x = value;
            case 1 -> vector.y = value;
            case 2 -> vector.z = value;
            default -> throw new IllegalArgumentException("Unknown axis " + axis);
        }
    }
}
