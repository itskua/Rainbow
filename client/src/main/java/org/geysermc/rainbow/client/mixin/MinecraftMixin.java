package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.geysermc.rainbow.client.imgui.ImGuiImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void rainbow$closeImGui(CallbackInfo ci) {
        ImGuiImpl.dispose();
    }
}
