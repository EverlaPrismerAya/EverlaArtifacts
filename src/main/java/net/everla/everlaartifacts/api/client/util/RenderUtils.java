package net.everla.everlaartifacts.api.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.inventory.InventoryMenu;
import org.lwjgl.opengl.GL13;

/**
 * Render utility methods for custom shaders.
 * <p>
 * Based on Avaritia's RenderUtils implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2024-2026 Nova-Committee
 */
public class RenderUtils {

    /**
     * Custom TextureStateShard that ensures Sampler0 (GL_TEXTURE0)
     * is bound to the block atlas before the cosmic shader renders.
     * This is needed because the cosmic shader uses Sampler0 for
     * both the mask texture and the cosmic star sprites.
     */
    public static final RenderStateShard.TextureStateShard COSMIC_TEXTURE_ISOLATED = new RenderStateShard.TextureStateShard(
            InventoryMenu.BLOCK_ATLAS, false, false) {
        @Override
        public void setupRenderState() {
            super.setupRenderState();
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);

            Minecraft mc = Minecraft.getInstance();
            TextureAtlas textureAtlas = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);

            RenderSystem.bindTexture(textureAtlas.getId());
        }

        @Override
        public void clearRenderState() {
            super.clearRenderState();
        }
    };
}
