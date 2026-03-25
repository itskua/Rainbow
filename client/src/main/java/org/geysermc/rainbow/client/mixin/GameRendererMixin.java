package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.geysermc.rainbow.client.editor.RainbowOverlay;
import org.geysermc.rainbow.client.imgui.ImGuiImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At("RETURN"))
    private void rainbow$renderImGui(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        RainbowOverlay overlay = RainbowOverlay.getInstance();
        if (!overlay.shouldRender(minecraft) || !ImGuiImpl.beginFrame(minecraft)) {
            return;
        }

        overlay.render(minecraft);
        ImGuiImpl.endFrame();
    }
}
