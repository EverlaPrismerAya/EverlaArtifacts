package net.everla.everlaartifacts.client.model.loader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.everla.everlaartifacts.api.client.model.bakedmodels.WrappedItemModel;
import net.everla.everlaartifacts.api.client.util.TransformUtils;
import net.everla.everlaartifacts.client.shader.EverlaArtifactsRenderTypes;
import net.everla.everlaartifacts.client.shader.EverlaArtifactsShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.List;

import static net.everla.everlaartifacts.client.shader.EverlaArtifactsShaders.COSMIC_UVS;

/**
 * Baked model for items with the cosmic starry sky effect.
 * Renders the base item model first, then applies the cosmic shader
 * over the mask texture areas.
 * <p>
 * Based on Avaritia's CosmicBakeModel implementation.
 *
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2024-2026 Nova-Committee
 */
public class CosmicBakeModel extends WrappedItemModel {

    private final List<ResourceLocation> maskSprites;

    public CosmicBakeModel(final BakedModel wrapped, final List<ResourceLocation> maskSprites) {
        super(wrapped);
        this.maskSprites = maskSprites;
        this.cosmic = true;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack,
                           MultiBufferSource source, int packedLight, int packedOverlay,
                           ItemModelShaper itemModelShaper, TextureManager textureManager) {

        // Choose the appropriate transform state based on item type
        if (stack.getItem() instanceof SwordItem) {
            this.parentState = TransformUtils.DEFAULT_TOOL;
        } else {
            this.parentState = TransformUtils.DEFAULT_ITEM;
        }

        // === Pass 1: Render the base model (the sword) ===
        this.renderWrapped(stack, pStack, source, packedLight, packedOverlay, true);

        // Flush the batch so the base model is drawn before the cosmic overlay
        if (source instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }

        // === Pass 2: Cosmic starry sky effect ===
        final Minecraft mc = Minecraft.getInstance();

        // Compute shader parameters
        float yaw = 0.0f;
        float pitch = 0.0f;
        float scale = 1.0f;

        if (transformType == ItemDisplayContext.GUI) {
            // In GUI/inventory, show the full star sphere at once
            scale = 100.0F;
        } else {
            // In-world: rotate star sphere with the player's view
            if (mc.player != null) {
                yaw = (float) (mc.player.getYRot() * 2.0f * Math.PI / 360.0);
                pitch = -(float) (mc.player.getXRot() * 2.0f * Math.PI / 360.0);
            }
        }

        // Set all shader uniforms
        if (mc.level != null) {
            EverlaArtifactsShaders.cosmicTime.set(mc.level.getGameTime() % Integer.MAX_VALUE);
        }
        EverlaArtifactsShaders.cosmicYaw.set(yaw);
        EverlaArtifactsShaders.cosmicPitch.set(pitch);
        EverlaArtifactsShaders.cosmicExternalScale.set(scale);
        EverlaArtifactsShaders.cosmicOpacity.set(1.0F);

        if (EverlaArtifactsShaders.cosmicUVs != null) {
            EverlaArtifactsShaders.cosmicUVs.set(COSMIC_UVS);
        }

        // Build mask quads from sprite(s) and render with the cosmic shader
        final VertexConsumer cons = source.getBuffer(EverlaArtifactsRenderTypes.COSMIC);
        List<TextureAtlasSprite> atlasSprites = new ArrayList<>();
        for (ResourceLocation res : maskSprites) {
            atlasSprites.add(Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(res));
        }
        mc.getItemRenderer().renderQuadList(pStack, cons, bakeItem(atlasSprites),
                stack, packedLight, packedOverlay);
    }
}
