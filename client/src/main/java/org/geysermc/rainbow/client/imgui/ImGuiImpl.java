package org.geysermc.rainbow.client.imgui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import org.geysermc.rainbow.Rainbow;
import org.lwjgl.glfw.GLFW;

public final class ImGuiImpl {
    private static final ImGuiImplGlfw IM_GUI_GLFW = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 IM_GUI_GL3 = new ImGuiImplGl3();
    private static boolean initialized;
    private static boolean failed;

    private ImGuiImpl() {
    }

    public static boolean beginFrame(Minecraft minecraft) {
        if (!ensureInitialized(minecraft)) {
            return false;
        }

        IM_GUI_GLFW.newFrame();
        IM_GUI_GL3.newFrame();
        ImGui.newFrame();
        return true;
    }

    public static void endFrame() {
        if (!initialized) {
            return;
        }

        ImGui.render();
        IM_GUI_GL3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            long pointer = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(pointer);
        }
    }

    public static void dispose() {
        if (!initialized) {
            return;
        }

        IM_GUI_GL3.shutdown();
        IM_GUI_GLFW.shutdown();
        ImGui.destroyContext();
        initialized = false;
        failed = false;
    }

    private static boolean ensureInitialized(Minecraft minecraft) {
        if (initialized) {
            return true;
        }
        if (failed || minecraft == null) {
            return false;
        }

        try {
            ImGui.createContext();
            ImGuiIO io = ImGui.getIO();
            io.setIniFilename("rainbow-imgui.ini");
            io.setConfigFlags(ImGuiConfigFlags.DockingEnable);

            long handle = minecraft.getWindow().handle();
            IM_GUI_GLFW.init(handle, true);
            IM_GUI_GL3.init("#version 150");
            initialized = true;
            Rainbow.LOGGER.info("Rainbow ImGui initialized");
            return true;
        } catch (Throwable throwable) {
            failed = true;
            Rainbow.LOGGER.error("Failed to initialize Rainbow ImGui", throwable);
            return false;
        }
    }
}
