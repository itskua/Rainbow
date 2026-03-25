package org.geysermc.rainbow.mapping.animation;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.geysermc.rainbow.pack.animation.BedrockAnimation;
import org.joml.Vector3f;
import org.joml.Vector3fc;

// TODO these offset values are still not entirely right, I think
public class AnimationMapper {
    private static final Vector3fc FIRST_PERSON_POSITION_OFFSET = new Vector3f(-7.0F, 22.5F, -7.0F);
    private static final Vector3fc FIRST_PERSON_ROTATION_OFFSET = new Vector3f(-22.5F, 50.0F, -32.5F);
    private static final Vector3fc FIRST_PERSON_GRIP_OFFSET = new Vector3f(0.0F, -1.5F, 1.5F);
    private static final Vector3fc THIRD_PERSON_GRIP_OFFSET = new Vector3f(0.0F, -1.5F, 1.5F);

    // These transformations aren't perfect... but I spent over 4 hours trying to get these. It's good enough for me.
    public static BedrockAnimationContext mapAnimation(String identifier, String bone, ItemTransforms transforms) {
        ItemTransformOverrides overrides = TransformOverrideRegistry.get(identifier);
        ItemTransformOverrides finalTransforms = applyOverrides(baseTransforms(transforms), overrides);

        TransformAdjustment firstPerson = finalTransforms.firstPerson();
        TransformAdjustment thirdPerson = finalTransforms.thirdPerson();
        TransformAdjustment head = finalTransforms.head();

        // Note that for items marked as equippable, the 3D model only shows up when having the item equipped on the head, and the icon is used when holding the item in hand
        // Interestingly when an item is NOT marked equippable (so the player can't equip it normally), the 3D model does show up properly both in hand and on head
        // I think this is a bedrock bug and not something we can fix
        return new BedrockAnimationContext(BedrockAnimation.builder()
                .withAnimation(identifier + ".hold_first_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, firstPerson.position(), firstPerson.rotation(), firstPerson.scale()))
                .withAnimation(identifier + ".hold_third_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, thirdPerson.position(), thirdPerson.rotation(), thirdPerson.scale()))
                .withAnimation(identifier + ".head", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, head.position(), head.rotation(), head.scale()))
                .build(), "animation." + identifier + ".hold_first_person", "animation." + identifier + ".hold_third_person", "animation." + identifier + ".head");
    }

    public static ItemTransformOverrides baseTransforms(ItemTransforms transforms) {
        return applyOverrides(rawBaseTransforms(transforms), ConverterCalibrationRegistry.get());
    }

    public static ItemTransformOverrides rawBaseTransforms(ItemTransforms transforms) {
        // Note that translations are multiplied by 0.0625 after reading on Java, so we have to divide by that here

        // I don't think it's possible to display separate animations for left- and right hands
        ItemTransform firstPerson = transforms.firstPersonRightHand();
        Vector3f firstPersonPosition = firstPerson.translation().div(0.0625F, new Vector3f()).add(FIRST_PERSON_POSITION_OFFSET).add(FIRST_PERSON_GRIP_OFFSET);
        Vector3f firstPersonRotation = FIRST_PERSON_ROTATION_OFFSET.add(firstPerson.rotation(), new Vector3f());
        Vector3f firstPersonScale = new Vector3f(firstPerson.scale());

        ItemTransform thirdPerson = transforms.thirdPersonRightHand();
        // Translation Y/Z axes are swapped on bedrock, bedrock displays the model lower than Java does, and the X/Y axes (Java) is inverted on bedrock
        // Also the model appears lower on bedrock so we need to add 10 on the Y axis here
        Vector3f thirdPersonTranslation = thirdPerson.translation().div(0.0625F, new Vector3f());
        Vector3f thirdPersonPosition = new Vector3f(-thirdPersonTranslation.x(), 10.0F + thirdPersonTranslation.z(), -thirdPersonTranslation.y())
                .add(THIRD_PERSON_GRIP_OFFSET);
        // Rotation X/Y axes are inverted on bedrock, bedrock needs a +90-degree rotation on the X axis, and I couldn't figure out how the Z axis works
        Vector3f thirdPersonRotation = new Vector3f(-thirdPerson.rotation().x() + 90.0F, -thirdPerson.rotation().y(), 0.0F);
        Vector3f thirdPersonScale = new Vector3f(thirdPerson.scale());

        // Head translation + scale is scaled by around 0.655 (not perfect but close enough)
        ItemTransform head = transforms.head();
        // Add a base translation of 20 on the Y axis as bedrock displays the item at the player's feet
        // Translation is inverted on the X axis
        Vector3f headPosition = head.translation().div(0.0625F, new Vector3f()).mul(-0.655F, 0.655F, 0.655F).add(0.0F, 20.0F, 0.0F);
        // Rotation is inverted on the X and Y axis
        Vector3f headRotation = new Vector3f(-head.rotation().x(), -head.rotation().y(), head.rotation().z());
        Vector3f headScale = head.scale().mul(0.655F, new Vector3f());
        return new ItemTransformOverrides(
                new TransformAdjustment(firstPersonPosition, firstPersonRotation, firstPersonScale),
                new TransformAdjustment(thirdPersonPosition, thirdPersonRotation, thirdPersonScale),
                new TransformAdjustment(headPosition, headRotation, headScale)
        );
    }

    public static ItemTransformOverrides applyOverrides(ItemTransformOverrides base, ItemTransformOverrides overrides) {
        return new ItemTransformOverrides(
                applyAdjustment(base.firstPerson(), overrides.firstPerson()),
                applyAdjustment(base.thirdPerson(), overrides.thirdPerson()),
                applyAdjustment(base.head(), overrides.head())
        );
    }

    private static void applyAdjustment(Vector3f position, Vector3f rotation, Vector3f scale, TransformAdjustment adjustment) {
        Vector3fc adjustmentPosition = adjustment.position();
        Vector3fc adjustmentRotation = adjustment.rotation();
        Vector3fc adjustmentScale = adjustment.scale();

        position.add(adjustmentPosition.x(), adjustmentPosition.y(), adjustmentPosition.z());
        rotation.add(adjustmentRotation.x(), adjustmentRotation.y(), adjustmentRotation.z());
        scale.mul(adjustmentScale.x(), adjustmentScale.y(), adjustmentScale.z());
    }

    private static TransformAdjustment applyAdjustment(TransformAdjustment base, TransformAdjustment adjustment) {
        Vector3f position = new Vector3f(base.position());
        Vector3f rotation = new Vector3f(base.rotation());
        Vector3f scale = new Vector3f(base.scale());
        applyAdjustment(position, rotation, scale, adjustment);
        return new TransformAdjustment(position, rotation, scale);
    }
}
