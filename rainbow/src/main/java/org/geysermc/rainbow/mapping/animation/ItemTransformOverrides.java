package org.geysermc.rainbow.mapping.animation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ItemTransformOverrides(TransformAdjustment firstPerson, TransformAdjustment thirdPerson, TransformAdjustment head) {
    public static final ItemTransformOverrides DEFAULT = new ItemTransformOverrides(TransformAdjustment.DEFAULT, TransformAdjustment.DEFAULT, TransformAdjustment.DEFAULT);
    public static final Codec<ItemTransformOverrides> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    TransformAdjustment.CODEC.optionalFieldOf("first_person", TransformAdjustment.DEFAULT).forGetter(ItemTransformOverrides::firstPerson),
                    TransformAdjustment.CODEC.optionalFieldOf("third_person", TransformAdjustment.DEFAULT).forGetter(ItemTransformOverrides::thirdPerson),
                    TransformAdjustment.CODEC.optionalFieldOf("head", TransformAdjustment.DEFAULT).forGetter(ItemTransformOverrides::head)
            ).apply(instance, ItemTransformOverrides::new)
    );
}
