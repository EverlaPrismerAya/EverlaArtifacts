package net.everla.everlaartifacts.api.client.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Map;

/**
 * A simple {@link ModelState} implementation composed of multiple {@link Transformation}s,
 * one per {@link ItemDisplayContext}.
 * <p>
 * Based on Avaritia's PerspectiveModelState implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2026 Nova-Committee
 */
public class PerspectiveModelState implements ModelState {

    public static final PerspectiveModelState IDENTITY = new PerspectiveModelState(ImmutableMap.of());

    private final Map<ItemDisplayContext, Transformation> transforms;
    private final boolean isUvLocked;

    public PerspectiveModelState(Map<ItemDisplayContext, Transformation> transforms) {
        this(transforms, false);
    }

    public PerspectiveModelState(Map<ItemDisplayContext, Transformation> transforms, boolean isUvLocked) {
        this.transforms = ImmutableMap.copyOf(transforms);
        this.isUvLocked = isUvLocked;
    }

    public Transformation getTransform(ItemDisplayContext context) {
        return transforms.getOrDefault(context, Transformation.identity());
    }

    @Override
    public boolean isUvLocked() {
        return isUvLocked;
    }
}
