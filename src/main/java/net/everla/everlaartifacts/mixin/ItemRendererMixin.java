package net.everla.everlaartifacts.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.everla.everlaartifacts.api.client.model.PerspectiveModel;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ItemRenderer#render} to intercept models that implement
 * {@link PerspectiveModel}. When a PerspectiveModel is detected, the vanilla
 * rendering pipeline is bypassed in favor of the model's custom
 * {@link PerspectiveModel#renderItem} method.
 * <p>
 * This is the bridge between vanilla item rendering and the custom cosmic
 * shader rendering pipeline.
 * <p>
 * Based on Avaritia's ItemRendererMixin implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2026 Nova-Committee
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Shadow
    @Final
    private ItemModelShaper itemModelShaper;

    @Shadow
    @Final
    private TextureManager textureManager;

    @Inject(
            method = "render",
            at = @At(value = "INVOKE",
                     target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                     ordinal = 0)
    )
    public void everlaartifacts$onRenderItem(ItemStack stack, ItemDisplayContext context,
                                              boolean leftHand, PoseStack mStack,
                                              MultiBufferSource buffers, int packedLight,
                                              int packedOverlay, BakedModel modelIn,
                                              CallbackInfo ci) {
        if (modelIn instanceof PerspectiveModel model) {
            mStack.pushPose();
            final PerspectiveModel transformModel = (PerspectiveModel) model.applyTransform(
                    context, mStack, leftHand);
            mStack.translate(-0.5D, -0.5D, -0.5D);
            transformModel.renderItem(stack, context, mStack, buffers,
                    packedLight, packedOverlay, this.itemModelShaper, this.textureManager);
            mStack.popPose();
        }
    }
}
