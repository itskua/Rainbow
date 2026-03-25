package org.geysermc.rainbow.client.imgui;

import imgui.ImGuiIO;

@FunctionalInterface
public interface RenderInterface {
    void renderImGui(ImGuiIO io);
}
