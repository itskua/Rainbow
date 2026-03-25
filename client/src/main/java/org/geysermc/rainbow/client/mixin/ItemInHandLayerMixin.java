package org.geysermc.rainbow.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.client.editor.TransformEditorSession;
import org.geysermc.rainbow.mapping.animation.TransformAdjustment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    private static final TransformAdjustment PREVIEW_FIRST_PERSON = new TransformAdjustment(
            new org.joml.Vector3f(7.0F, -2.0F, 3.0F),
            new org.joml.Vector3f(-22.0F, 42.0F, -20.0F),
            new org.joml.Vector3f(1.0F, 1.0F, 1.0F)
    );
    private static final TransformAdjustment PREVIEW_THIRD_PERSON = new TransformAdjustment(
            new org.joml.Vector3f(0.0F, -1.5F, 1.5F),
            new org.joml.Vector3f(16.0F, -12.0F, 0.0F),
            new org.joml.Vector3f(1.0F, 1.0F, 1.0F)
    );
    private static final TransformAdjustment PREVIEW_HEAD = new TransformAdjustment(
            new org.joml.Vector3f(0.0F, 2.0F, 0.0F),
            new org.joml.Vector3f(0.0F, 0.0F, 0.0F),
            new org.joml.Vector3f(0.82F, 0.82F, 0.82F)
    );

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void rainbow$applyPreviewTransform(ArmedEntityRenderState state, ItemStackRenderState renderState, ItemStack stack, HumanoidArm arm,
                                               PoseStack poseStack, SubmitNodeCollector collector, int packedLight, CallbackInfo ci) {
        if (!TransformEditorSession.matches(stack)) {
            return;
        }

        if (TransformEditorSession.isPreviewRender()) {
            apply(poseStack, switch (TransformEditorSession.previewMode()) {
                case FIRST_PERSON -> PREVIEW_FIRST_PERSON;
                case THIRD_PERSON -> PREVIEW_THIRD_PERSON;
                case HEAD -> PREVIEW_HEAD;
            });
        }

        TransformAdjustment adjustment = TransformEditorSession.activeAdjustment();
        apply(poseStack, adjustment);
    }

    private static void apply(PoseStack poseStack, TransformAdjustment adjustment) {
        poseStack.translate(adjustment.position().x() / 16.0F, adjustment.position().y() / 16.0F, adjustment.position().z() / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(adjustment.rotation().x()));
        poseStack.mulPose(Axis.YP.rotationDegrees(adjustment.rotation().y()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(adjustment.rotation().z()));
        poseStack.scale(adjustment.scale().x(), adjustment.scale().y(), adjustment.scale().z());
    }
}
