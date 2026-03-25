package org.geysermc.rainbow.mapping.animation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.geysermc.rainbow.CodecUtil;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record TransformAdjustment(Vector3fc position, Vector3fc rotation, Vector3fc scale) {
    public static final TransformAdjustment DEFAULT = new TransformAdjustment(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
    public static final Codec<TransformAdjustment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.VECTOR3F_CODEC.optionalFieldOf("position", DEFAULT.position).forGetter(TransformAdjustment::position),
                    CodecUtil.VECTOR3F_CODEC.optionalFieldOf("rotation", DEFAULT.rotation).forGetter(TransformAdjustment::rotation),
                    CodecUtil.VECTOR3F_CODEC.optionalFieldOf("scale", DEFAULT.scale).forGetter(TransformAdjustment::scale)
            ).apply(instance, TransformAdjustment::new)
    );
}
