package org.geysermc.rainbow.client.editor;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImFloat;
import imgui.type.ImString;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.client.RainbowClient;
import org.geysermc.rainbow.client.mapper.InventoryMapper;
import org.geysermc.rainbow.pack.BedrockPack;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.ItemTransformOverrides;
import org.geysermc.rainbow.mapping.animation.TransformAdjustment;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;
import org.lwjgl.BufferUtils;

import java.util.Optional;

public final class RainbowOverlay {
    private enum EditorMode {
        ITEM("Item Editor"),
        DEV("Dev Calibration");

        private final String label;

        EditorMode(String label) {
            this.label = label;
        }
    }

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

    private static final RainbowOverlay INSTANCE = new RainbowOverlay();

    private final ImString packName = new ImString(64);
    private boolean visible = true;
    private boolean previewVisible = true;
    private String targetIdentifier;
    private EditorMode editorMode = EditorMode.ITEM;
    private ItemTransformOverrides overrides = ItemTransformOverrides.DEFAULT;
    private ItemTransformOverrides rawBaseTransforms = ItemTransformOverrides.DEFAULT;
    private ItemTransformOverrides baseTransforms = ItemTransformOverrides.DEFAULT;
    private ItemTransformOverrides finalTransforms = ItemTransformOverrides.DEFAULT;
    private ItemTransformOverrides devCalibration = ItemTransformOverrides.DEFAULT;
    private ItemTransformOverrides devFinalTransforms = ItemTransformOverrides.DEFAULT;
    private ViewMode mode = ViewMode.THIRD_PERSON;
    private String status = "Rainbow overlay ready";
    private int previewLeft;
    private int previewTop;
    private int previewRight;
    private int previewBottom;
    private boolean previewDragging;
    private float previewYaw = 22.0F;
    private float previewPitch = -8.0F;
    private boolean previousLeftDown;
    private double lastPreviewMouseX;
    private double lastPreviewMouseY;
    private final ImFloat[] positionInputs = createInputs(0.0F, 0.0F, 0.0F);
    private final ImFloat[] rotationInputs = createInputs(0.0F, 0.0F, 0.0F);
    private final ImFloat[] scaleInputs = createInputs(1.0F, 1.0F, 1.0F);

    private RainbowOverlay() {
    }

    public static RainbowOverlay getInstance() {
        return INSTANCE;
    }

    public boolean shouldRender(Minecraft minecraft) {
        return minecraft.player != null && visible;
    }

    public String targetIdentifier() {
        return targetIdentifier;
    }

    public boolean isPreviewVisible() {
        return previewVisible;
    }

    public boolean showCalibrationComparison() {
        return editorMode == EditorMode.DEV;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
        TransformEditorSession.close();
        previewDragging = false;
    }

    public void setPreviewBounds(int left, int top, int right, int bottom) {
        previewLeft = left;
        previewTop = top;
        previewRight = right;
        previewBottom = bottom;
    }

    public float previewYaw() {
        return previewYaw;
    }

    public float previewPitch() {
        return previewPitch;
    }

    public TransformAdjustment previewRawAdjustment() {
        return adjustmentFor(rawBaseTransforms);
    }

    public TransformAdjustment previewCalibratedAdjustment() {
        return adjustmentFor(devFinalTransforms);
    }

    public void updatePreviewInteraction(Minecraft minecraft) {
        if (!visible || !isPreviewInteractive()) {
            previewDragging = false;
            previousLeftDown = false;
            return;
        }

        long window = minecraft.getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        double mouseX = scaledMouseX(minecraft);
        double mouseY = scaledMouseY(minecraft);

        if (leftDown && !previousLeftDown && isInsidePreview(mouseX, mouseY)) {
            previewDragging = true;
            lastPreviewMouseX = mouseX;
            lastPreviewMouseY = mouseY;
        } else if (!leftDown) {
            previewDragging = false;
        }

        if (previewDragging) {
            double dragX = mouseX - lastPreviewMouseX;
            double dragY = mouseY - lastPreviewMouseY;
            previewYaw = clamp(previewYaw + (float) dragX * 0.75F, -80.0F, 80.0F);
            previewPitch = clamp(previewPitch + (float) dragY * 0.5F, -35.0F, 35.0F);
            lastPreviewMouseX = mouseX;
            lastPreviewMouseY = mouseY;
        }

        previousLeftDown = leftDown;
    }

    public void render(Minecraft minecraft) {
        syncHeldItem();

        ImGui.setNextWindowPos(18, 18, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(420, Math.max(460, minecraft.getWindow().getHeight() - 80), ImGuiCond.FirstUseEver);

        if (!ImGui.begin("Rainbow Overlay")) {
            ImGui.end();
            return;
        }

        ImGui.text("Live Bedrock item editor");
        ImGui.separator();

        renderPackActions(minecraft);
        ImGui.separator();
        renderEditorModeSwitch();
        ImGui.separator();
        renderEditor();
        ImGui.separator();
        ImGui.textWrapped(status);

        if (ImGui.button("Hide Overlay")) {
            hide();
        }

        ImGui.end();
    }

    private void renderPackActions(Minecraft minecraft) {
        PackManager packManager = RainbowClient.getInstance().getPackManager();

        ImGui.text("Pack");
        ImGui.inputText("Name", packName);
        ImGui.sameLine();
        if (ImGui.button("Create")) {
            if (packName.isEmpty()) {
                setStatus("Enter a pack name before creating one", true);
            } else {
                try {
                    packManager.startPack(packName.get());
                    setStatus("Created pack " + packName.get(), false);
                } catch (Exception exception) {
                    setStatus("Failed to create pack: " + exception.getMessage(), true);
                }
            }
        }

        if (packManager.getCurrentPackName().isPresent()) {
            ImGui.text("Current pack: " + packManager.getCurrentPackName().orElse("unknown"));
        } else {
            ImGui.text("Current pack: none");
        }

        if (ImGui.button("Map Held")) {
            mapHeld(minecraft);
        }
        ImGui.sameLine();
        if (ImGui.button("Map Inventory")) {
            mapInventory(minecraft);
        }

        if (ImGui.button("Auto Inventory")) {
            RainbowClient.getInstance().getPackMapper().setItemProvider(InventoryMapper.INSTANCE);
            setStatus("Automatic inventory mapping enabled", false);
        }
        ImGui.sameLine();
        if (ImGui.button("Stop Auto")) {
            RainbowClient.getInstance().getPackMapper().setItemProvider(null);
            setStatus("Automatic mapping stopped", false);
        }
        ImGui.sameLine();
        if (ImGui.button("Finish")) {
            boolean finished = packManager.finish(() -> setStatus("Pack export finished", false));
            if (!finished) {
                setStatus("Create a pack before finishing", true);
            } else {
                setStatus("Finishing export...", false);
            }
        }
    }

    private void renderEditorModeSwitch() {
        ImGui.text("Workspace");
        if (ImGui.button("Item Editor")) {
            switchEditorMode(EditorMode.ITEM);
        }
        ImGui.sameLine();
        if (ImGui.button("Dev Calibration")) {
            switchEditorMode(EditorMode.DEV);
        }
        ImGui.textWrapped(switch (editorMode) {
            case ITEM -> "Per-item export override. Use this after mapping to fix one specific item.";
            case DEV -> "Repo/dev-only converter calibration. Use this to tune Rainbow's global Bedrock base placement.";
        });
    }

    private void renderEditor() {
        ImGui.text(editorMode.label);

        if (targetIdentifier == null) {
            ImGui.textWrapped("Hold a mapped custom item with an item model to edit its transform live.");
            return;
        }

        ImGui.text(targetIdentifier);
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

        ImGui.text("Editing: " + editorMode.label + " / " + mode.label);
        renderAxisGuide();
        renderVectorEditor("Position", "position", 0.5F);
        renderVectorEditor("Rotation", "rotation", 2.5F);
        renderVectorEditor("Scale", "scale", 0.05F);

        if (editorMode == EditorMode.ITEM) {
            renderCurrentValues("Base", baseAdjustment());
            renderCurrentValues("Override", overrideAdjustment());
            renderCurrentValues("Final", displayedAdjustment());
        } else {
            renderCurrentValues("Raw Base", rawBaseAdjustment());
            renderCurrentValues("Calibration", overrideAdjustment());
            renderCurrentValues("Calibrated", displayedAdjustment());
        }

        if (ImGui.button(editorMode == EditorMode.ITEM ? "Save Item Override" : "Save Dev Calibration")) {
            saveActiveEditor();
        }
        ImGui.sameLine();
        if (ImGui.button(editorMode == EditorMode.ITEM ? "Reset Item Override" : "Reset Dev Calibration")) {
            resetActiveEditor();
        }
        ImGui.sameLine();
        if (ImGui.button("Dump Bedrock Values")) {
            var path = BedrockPreviewDump.save(new BedrockPreviewDump(
                    targetIdentifier,
                    rawBaseTransforms,
                    devCalibration,
                    baseTransforms,
                    overrides,
                    finalTransforms
            ));
            setStatus("Saved Bedrock preview dump to " + path, false);
        }
        ImGui.sameLine();
        if (ImGui.button(previewVisible ? "Hide Preview" : "Show Preview")) {
            previewVisible = !previewVisible;
        }
    }

    private void renderCurrentValues(String label, TransformAdjustment adjustment) {
        ImGui.textWrapped(label + " | Pos "
                + formatVector(adjustment.position())
                + " | Rot "
                + formatVector(adjustment.rotation())
                + " | Scale "
                + formatVector(adjustment.scale()));
    }

    private void renderVectorEditor(String title, String group, float step) {
        ImGui.text(title);
        renderAxisRow("X", group, 0, step);
        renderAxisRow("Y", group, 1, step);
        renderAxisRow("Z", group, 2, step);
        ImGui.spacing();
    }

    private void renderAxisRow(String label, String group, int axis, float step) {
        ImGui.text(label + " " + axisMeaning(group, axis));
        ImGui.sameLine();
        if (ImGui.button("-##" + group + axis)) {
            adjust(group, axis, -step);
        }
        ImGui.sameLine();
        ImGui.setNextItemWidth(90.0F);
        ImFloat input = inputFor(group, axis);
        if (ImGui.inputFloat("##" + group + axis, input, 0.0F, 0.0F, "%.3f")) {
            setExact(group, axis, input.get());
        }
        ImGui.sameLine();
        if (ImGui.button("+##" + group + axis)) {
            adjust(group, axis, step);
        }
    }

    private void renderAxisGuide() {
        if (editorMode == EditorMode.DEV) {
            ImGui.textWrapped("Developer calibration edits Rainbow's global Java-to-Bedrock base transform. Item overrides are applied later on top of this.");
        }
        ImGui.textWrapped(switch (mode) {
            case FIRST_PERSON ->
                    "First Person: Position X=up/down, Y=distance, Z=left/right. Rotation X=tilt, Y=roll, Z=pan. Scale X=width, Y=height, Z=depth.";
            case THIRD_PERSON ->
                    "Third Person: Position X=left/right, Y=up/down grip, Z=forward/back from player. Rotation X=tilt, Y=roll, Z=pan. Scale X=width, Y=height, Z=depth.";
            case HEAD ->
                    "Head: Position moves the equipped model on the head slot. Rotation spins the head item. Scale changes overall item size.";
        });
        ImGui.spacing();
    }

    private void switchMode(ViewMode mode) {
        this.mode = mode;
        TransformEditorSession.setPreviewMode(mode.previewMode);
        if (mode == ViewMode.FIRST_PERSON) {
            previewYaw = 58.0F;
            previewPitch = -10.0F;
        } else if (mode == ViewMode.HEAD) {
            previewYaw = 8.0F;
            previewPitch = -4.0F;
        } else if (mode == ViewMode.THIRD_PERSON) {
            previewYaw = 22.0F;
            previewPitch = -8.0F;
        }
        recalculateDisplayedTransforms();
    }

    private void switchEditorMode(EditorMode mode) {
        editorMode = mode;
        updateSessionOverrides();
        recalculateDisplayedTransforms();
    }

    private void adjust(String group, int axis, float amount) {
        setExact(group, axis, currentValue(group, axis) + amount);
    }

    private void setExact(String group, int axis, float value) {
        TransformAdjustment displayed = displayedAdjustment();
        TransformAdjustment base = baseAdjustment();
        TransformAdjustment currentOverride = overrideAdjustment();

        Vector3f displayedPosition = new Vector3f(displayed.position());
        Vector3f displayedRotation = new Vector3f(displayed.rotation());
        Vector3f displayedScale = new Vector3f(displayed.scale());

        Vector3f displayedTarget = switch (group) {
            case "position" -> displayedPosition;
            case "rotation" -> displayedRotation;
            case "scale" -> displayedScale;
            default -> throw new IllegalArgumentException("Unknown transform group " + group);
        };
        setAxis(displayedTarget, axis, value);

        TransformAdjustment updated = switch (group) {
            case "position" -> new TransformAdjustment(
                    subtract(displayedPosition, base.position()),
                    new Vector3f(currentOverride.rotation()),
                    new Vector3f(currentOverride.scale())
            );
            case "rotation" -> new TransformAdjustment(
                    new Vector3f(currentOverride.position()),
                    subtract(displayedRotation, base.rotation()),
                    new Vector3f(currentOverride.scale())
            );
            case "scale" -> new TransformAdjustment(
                    new Vector3f(currentOverride.position()),
                    new Vector3f(currentOverride.rotation()),
                    divide(displayedScale, base.scale())
            );
            default -> throw new IllegalArgumentException("Unknown transform group " + group);
        };

        if (editorMode == EditorMode.ITEM) {
            overrides = switch (mode) {
                case FIRST_PERSON -> new ItemTransformOverrides(updated, overrides.thirdPerson(), overrides.head());
                case THIRD_PERSON -> new ItemTransformOverrides(overrides.firstPerson(), updated, overrides.head());
                case HEAD -> new ItemTransformOverrides(overrides.firstPerson(), overrides.thirdPerson(), updated);
            };
        } else {
            devCalibration = switch (mode) {
                case FIRST_PERSON -> new ItemTransformOverrides(updated, devCalibration.thirdPerson(), devCalibration.head());
                case THIRD_PERSON -> new ItemTransformOverrides(devCalibration.firstPerson(), updated, devCalibration.head());
                case HEAD -> new ItemTransformOverrides(devCalibration.firstPerson(), devCalibration.thirdPerson(), updated);
            };
        }
        updateSessionOverrides();
        recalculateDisplayedTransforms();
    }

    private float currentValue(String group, int axis) {
        TransformAdjustment adjustment = displayedAdjustment();
        Vector3fc vector = switch (group) {
            case "position" -> adjustment.position();
            case "rotation" -> adjustment.rotation();
            case "scale" -> adjustment.scale();
            default -> throw new IllegalArgumentException("Unknown transform group " + group);
        };
        return getAxis(vector, axis);
    }

    private TransformAdjustment displayedAdjustment() {
        ItemTransformOverrides active = editorMode == EditorMode.ITEM ? finalTransforms : devFinalTransforms;
        return adjustmentFor(active);
    }

    private TransformAdjustment baseAdjustment() {
        ItemTransformOverrides active = editorMode == EditorMode.ITEM ? baseTransforms : rawBaseTransforms;
        return adjustmentFor(active);
    }

    private TransformAdjustment rawBaseAdjustment() {
        return adjustmentFor(rawBaseTransforms);
    }

    private TransformAdjustment overrideAdjustment() {
        ItemTransformOverrides active = editorMode == EditorMode.ITEM ? overrides : devCalibration;
        return adjustmentFor(active);
    }

    private void syncHeldItem() {
        Optional<String> identifier = TransformOverrideManager.identifierForHeldItem();
        if (identifier.isEmpty()) {
            targetIdentifier = null;
            TransformEditorSession.close();
            return;
        }

        String nextIdentifier = identifier.get();
        if (!nextIdentifier.equals(targetIdentifier)) {
            targetIdentifier = nextIdentifier;
            overrides = TransformOverrideManager.get(targetIdentifier);
            devCalibration = ConverterCalibrationManager.get();
            TransformEditorSession.open(targetIdentifier, activeEditorOverrides());
            TransformEditorSession.setPreviewMode(mode.previewMode);
            recalculateDisplayedTransforms();
            setStatus("Editing held item " + targetIdentifier, false);
        } else if (!TransformEditorSession.isActive()) {
            devCalibration = ConverterCalibrationManager.get();
            TransformEditorSession.open(targetIdentifier, activeEditorOverrides());
            TransformEditorSession.setPreviewMode(mode.previewMode);
            recalculateDisplayedTransforms();
        }
    }

    private void mapHeld(Minecraft minecraft) {
        ItemStack heldItem = minecraft.player.getMainHandItem();
        PackManager packManager = RainbowClient.getInstance().getPackManager();
        packManager.runOrElse(pack -> {
            BedrockPack.MappingResult result = pack.map(heldItem);
            switch (result) {
                case NONE_MAPPED -> setStatus("No held item could be mapped", true);
                case PROBLEMS_OCCURRED -> setStatus("Held item mapped with warnings", true);
                case MAPPED_SUCCESSFULLY -> setStatus("Held item mapped successfully", false);
            }
        }, () -> setStatus("Create a pack before mapping items", true));
    }

    private void refreshMappedHeldItem() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ItemStack heldItem = minecraft.player.getMainHandItem();
        if (heldItem.isEmpty()) {
            return;
        }

        RainbowClient.getInstance().getPackManager().run(pack -> {
            BedrockPack.MappingResult result = pack.forceRemap(heldItem);
            if (result != BedrockPack.MappingResult.NONE_MAPPED) {
                setStatus("Saved and refreshed export mapping for " + targetIdentifier, false);
            }
        });
    }

    private void mapInventory(Minecraft minecraft) {
        Inventory inventory = minecraft.player.getInventory();
        PackManager packManager = RainbowClient.getInstance().getPackManager();
        packManager.runOrElse(pack -> {
            int mapped = 0;
            boolean errors = false;
            for (ItemStack stack : inventory) {
                BedrockPack.MappingResult result = pack.map(stack);
                if (result != BedrockPack.MappingResult.NONE_MAPPED) {
                    mapped++;
                    if (result == BedrockPack.MappingResult.PROBLEMS_OCCURRED) {
                        errors = true;
                    }
                }
            }

            if (mapped == 0) {
                setStatus("No items from your inventory were mapped", true);
            } else if (errors) {
                setStatus("Mapped " + mapped + " inventory items with warnings", true);
            } else {
                setStatus("Mapped " + mapped + " inventory items", false);
            }
        }, () -> setStatus("Create a pack before mapping items", true));
    }

    private void setStatus(String message, boolean error) {
        status = (error ? "[warn] " : "[ok] ") + message;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(message)
                    .withStyle(error ? ChatFormatting.RED : ChatFormatting.GREEN), true);
        }
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

    private ImFloat inputFor(String group, int axis) {
        return switch (group) {
            case "position" -> positionInputs[axis];
            case "rotation" -> rotationInputs[axis];
            case "scale" -> scaleInputs[axis];
            default -> throw new IllegalArgumentException("Unknown transform group " + group);
        };
    }

    private void refreshInputs() {
        TransformAdjustment active = displayedAdjustment();
        setInputs(positionInputs, active.position());
        setInputs(rotationInputs, active.rotation());
        setInputs(scaleInputs, active.scale());
    }

    private void recalculateDisplayedTransforms() {
        rawBaseTransforms = TransformOverrideManager.rawBaseTransformsForHeldItem().orElse(ItemTransformOverrides.DEFAULT);
        devCalibration = ConverterCalibrationManager.get();
        devFinalTransforms = AnimationMapper.applyOverrides(rawBaseTransforms, devCalibration);
        baseTransforms = TransformOverrideManager.baseTransformsForHeldItem().orElse(ItemTransformOverrides.DEFAULT);
        finalTransforms = AnimationMapper.applyOverrides(baseTransforms, overrides);
        refreshInputs();
    }

    private void updateSessionOverrides() {
        TransformEditorSession.setOverrides(activeEditorOverrides());
    }

    private ItemTransformOverrides activeEditorOverrides() {
        return editorMode == EditorMode.ITEM ? overrides : devCalibration;
    }

    private void saveActiveEditor() {
        if (editorMode == EditorMode.ITEM) {
            TransformOverrideManager.put(targetIdentifier, overrides);
            refreshMappedHeldItem();
            setStatus("Saved item override for " + targetIdentifier, false);
            return;
        }

        ConverterCalibrationManager.put(devCalibration);
        recalculateDisplayedTransforms();
        refreshMappedHeldItem();
        setStatus("Saved dev calibration for Rainbow base transforms", false);
    }

    private void resetActiveEditor() {
        if (editorMode == EditorMode.ITEM) {
            overrides = ItemTransformOverrides.DEFAULT;
            TransformOverrideManager.reset(targetIdentifier);
            updateSessionOverrides();
            recalculateDisplayedTransforms();
            refreshMappedHeldItem();
            setStatus("Reset item override for " + targetIdentifier, false);
            return;
        }

        devCalibration = ItemTransformOverrides.DEFAULT;
        ConverterCalibrationManager.reset();
        updateSessionOverrides();
        recalculateDisplayedTransforms();
        refreshMappedHeldItem();
        setStatus("Reset Rainbow dev calibration", false);
    }

    private static void setInputs(ImFloat[] inputs, Vector3fc vector) {
        inputs[0].set(vector.x());
        inputs[1].set(vector.y());
        inputs[2].set(vector.z());
    }

    private static ImFloat[] createInputs(float x, float y, float z) {
        return new ImFloat[] {
                new ImFloat(x),
                new ImFloat(y),
                new ImFloat(z)
        };
    }

    private static Vector3f subtract(Vector3fc value, Vector3fc base) {
        return new Vector3f(value.x() - base.x(), value.y() - base.y(), value.z() - base.z());
    }

    private static Vector3f divide(Vector3fc value, Vector3fc base) {
        return new Vector3f(
                safeDivide(value.x(), base.x()),
                safeDivide(value.y(), base.y()),
                safeDivide(value.z(), base.z())
        );
    }

    private static float safeDivide(float value, float base) {
        return base == 0.0F ? value : value / base;
    }

    private TransformAdjustment adjustmentFor(ItemTransformOverrides transforms) {
        return switch (mode) {
            case FIRST_PERSON -> transforms.firstPerson();
            case THIRD_PERSON -> transforms.thirdPerson();
            case HEAD -> transforms.head();
        };
    }

    private static String formatVector(Vector3fc vector) {
        return "[" + formatFloat(vector.x()) + ", " + formatFloat(vector.y()) + ", " + formatFloat(vector.z()) + "]";
    }

    private static String formatFloat(float value) {
        return String.format("%.3f", value);
    }

    private String axisMeaning(String group, int axis) {
        return switch (mode) {
            case FIRST_PERSON -> switch (group) {
                case "position" -> switch (axis) {
                    case 0 -> "(up/down)";
                    case 1 -> "(distance)";
                    case 2 -> "(left/right)";
                    default -> "";
                };
                case "rotation" -> switch (axis) {
                    case 0 -> "(tilt)";
                    case 1 -> "(roll)";
                    case 2 -> "(pan)";
                    default -> "";
                };
                case "scale" -> switch (axis) {
                    case 0 -> "(width)";
                    case 1 -> "(height)";
                    case 2 -> "(depth)";
                    default -> "";
                };
                default -> "";
            };
            case THIRD_PERSON -> switch (group) {
                case "position" -> switch (axis) {
                    case 0 -> "(left/right)";
                    case 1 -> "(grip up/down)";
                    case 2 -> "(forward/back)";
                    default -> "";
                };
                case "rotation" -> switch (axis) {
                    case 0 -> "(tilt)";
                    case 1 -> "(roll)";
                    case 2 -> "(pan)";
                    default -> "";
                };
                case "scale" -> switch (axis) {
                    case 0 -> "(width)";
                    case 1 -> "(height)";
                    case 2 -> "(depth)";
                    default -> "";
                };
                default -> "";
            };
            case HEAD -> switch (group) {
                case "position" -> switch (axis) {
                    case 0 -> "(left/right)";
                    case 1 -> "(up/down)";
                    case 2 -> "(forward/back)";
                    default -> "";
                };
                case "rotation" -> switch (axis) {
                    case 0 -> "(tilt)";
                    case 1 -> "(roll)";
                    case 2 -> "(pan)";
                    default -> "";
                };
                case "scale" -> switch (axis) {
                    case 0 -> "(width)";
                    case 1 -> "(height)";
                    case 2 -> "(depth)";
                    default -> "";
                };
                default -> "";
            };
        };
    }

    private boolean isPreviewInteractive() {
        return TransformEditorSession.previewMode() != TransformEditorSession.PreviewMode.FIRST_PERSON;
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return mouseX >= previewLeft && mouseX <= previewRight && mouseY >= previewTop && mouseY <= previewBottom;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double scaledMouseX(Minecraft minecraft) {
        DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer y = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(minecraft.getWindow().handle(), x, y);
        return x.get(0) * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getWidth();
    }

    private static double scaledMouseY(Minecraft minecraft) {
        DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer y = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(minecraft.getWindow().handle(), x, y);
        return y.get(0) * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getHeight();
    }
}
