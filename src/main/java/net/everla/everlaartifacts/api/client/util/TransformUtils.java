package net.everla.everlaartifacts.api.client.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.math.Transformation;
import net.everla.everlaartifacts.api.client.model.PerspectiveModelState;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating {@link Transformation}s and
 * {@link PerspectiveModelState}s for item models.
 * <p>
 * Based on Avaritia's TransformUtils implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2026 Nova-Committee
 */
public class TransformUtils {

    public static final PerspectiveModelState IDENTITY = PerspectiveModelState.IDENTITY;

    public static final PerspectiveModelState DEFAULT_BLOCK;
    public static final PerspectiveModelState DEFAULT_ITEM;
    public static final PerspectiveModelState DEFAULT_TOOL;
    public static final PerspectiveModelState DEFAULT_BOW;

    private static final Transformation flipX = new Transformation(null, null, new Vector3f(-1, 1, 1), null);

    static {
        Map<ItemDisplayContext, Transformation> map;
        Transformation thirdPerson;
        Transformation firstPerson;

        // Default block transforms
        map = new HashMap<>();
        thirdPerson = create(0F, 2.5F, 0F, 75F, 45F, 0F, 0.375F);
        map.put(ItemDisplayContext.GUI, create(0F, 0F, 0F, 30F, 225F, 0F, 0.625F));
        map.put(ItemDisplayContext.GROUND, create(0F, 3F, 0F, 0F, 0F, 0F, 0.25F));
        map.put(ItemDisplayContext.FIXED, create(0F, 0F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, thirdPerson);
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, flipLeft(thirdPerson));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(0F, 0F, 0F, 0F, 45F, 0F, 0.4F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(0F, 0F, 0F, 0F, 225F, 0F, 0.4F));
        DEFAULT_BLOCK = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // Default item transforms
        map = new HashMap<>();
        thirdPerson = create(0F, 3F, 1F, 0F, 0F, 0F, 0.55F);
        firstPerson = create(1.13F, 3.2F, 1.13F, 0F, -90F, 25F, 0.68F);
        map.put(ItemDisplayContext.GROUND, create(0F, 2F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.HEAD, create(0F, 13F, 7F, 0F, 180F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, thirdPerson);
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, flipLeft(thirdPerson));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, firstPerson);
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, flipLeft(firstPerson));
        DEFAULT_ITEM = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // Default tool transforms
        map = new HashMap<>();
        map.put(ItemDisplayContext.GROUND, create(0F, 2F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.FIXED, create(0F, 0F, 0F, 0F, 180F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, create(0F, 4F, 0.5F, 0F, -90F, 55F, 0.85F));
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, create(0F, 4F, 0.5F, 0F, 90F, -55F, 0.85F));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(1.13F, 3.2F, 1.13F, 0F, -90F, 25F, 0.68F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(1.13F, 3.2F, 1.13F, 0F, 90F, -25F, 0.68F));
        DEFAULT_TOOL = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // Default bow transforms
        map = new HashMap<>();
        map.put(ItemDisplayContext.GROUND, create(0F, 2F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.FIXED, create(0F, 0F, 0F, 0F, 180F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, create(-1F, -2F, 2.5F, -80F, 260F, -40F, 0.9F));
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, create(-1F, -2F, 2.5F, -80F, -280F, 40F, 0.9F));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(1.13F, 3.2F, 1.13F, 0F, -90F, 25F, 0.68F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(1.13F, 3.2F, 1.13F, 0F, 90F, -25F, 0.68F));
        DEFAULT_BOW = new PerspectiveModelState(ImmutableMap.copyOf(map));
    }

    /**
     * Creates a new Transformation from TRSR parameters.
     */
    public static Transformation create(float tx, float ty, float tz, float rx, float ry, float rz, float s) {
        return create(new Vector3f(tx / 16, ty / 16, tz / 16),
                new Vector3f(rx, ry, rz),
                new Vector3f(s, s, s));
    }

    /**
     * Creates a new Transformation from separate vectors.
     */
    public static Transformation create(Vector3f transform, Vector3f rotation, Vector3f scale) {
        return new Transformation(
                transform,
                new Quaternionf().rotationXYZ(
                        (float) (rotation.x() * Math.PI / 180.0),
                        (float) (rotation.y() * Math.PI / 180.0),
                        (float) (rotation.z() * Math.PI / 180.0)),
                scale,
                null
        );
    }

    public static Transformation create(ItemTransform transform) {
        if (ItemTransform.NO_TRANSFORM.equals(transform)) return Transformation.identity();
        return create(transform.translation, transform.rotation, transform.scale);
    }

    /**
     * Flips the transform for the left hand.
     */
    public static Transformation flipLeft(Transformation transform) {
        return flipX.compose(transform).compose(flipX);
    }

    /**
     * Decompose a vanilla {@link ItemTransforms} into a {@link PerspectiveModelState}.
     */
    public static PerspectiveModelState stateFromItemTransforms(ItemTransforms itemTransforms) {
        if (itemTransforms == ItemTransforms.NO_TRANSFORMS) return IDENTITY;

        ImmutableMap.Builder<ItemDisplayContext, Transformation> map = ImmutableMap.builder();
        for (ItemDisplayContext value : ItemDisplayContext.values()) {
            map.put(value, create(itemTransforms.getTransform(value)));
        }
        return new PerspectiveModelState(map.build());
    }
}
