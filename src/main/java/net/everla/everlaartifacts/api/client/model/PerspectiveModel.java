package net.everla.everlaartifacts.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;

/**
 * Interface for baked models that use custom perspective transforms and
 * custom rendering (instead of vanilla quad-based rendering).
 * <p>
 * Based on Avaritia's PerspectiveModel implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2026 Nova-Committee
 */
public interface PerspectiveModel extends BakedModel {

    @Nullable
    PerspectiveModelState getModelState();

    void renderItem(ItemStack stack, ItemDisplayContext ctx, PoseStack mStack, MultiBufferSource source,
                    int packedLight, int packedOverlay,
                    ItemModelShaper itemModelShaper, TextureManager textureManager);

    // Standard overrides for custom rendered models
    @Override
    default @NotNull List<BakedQuad> getQuads(BlockState state, Direction side, @NotNull RandomSource rand) {
        return Collections.emptyList();
    }

    @Override
    default boolean isCustomRenderer() {
        return true;
    }

    @Override
    default @NotNull TextureAtlasSprite getParticleIcon() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(new ResourceLocation("missingno"));
    }

    @Override
    default @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    default @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext context, @NotNull PoseStack pStack,
                                                boolean leftFlip) {
        PerspectiveModelState modelState = getModelState();
        if (modelState != null) {
            Transformation transform = getModelState().getTransform(context);
            Vector3f trans = transform.getTranslation();
            Vector3f scale = transform.getScale();
            pStack.translate(trans.x(), trans.y(), trans.z());
            pStack.mulPose(transform.getLeftRotation());
            pStack.scale(scale.x(), scale.y(), scale.z());
            pStack.mulPose(transform.getRightRotation());

            if (leftFlip) {
                pStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(180.0f));
            }
            return this;
        }
        return BakedModel.super.applyTransform(context, pStack, leftFlip);
    }
}
