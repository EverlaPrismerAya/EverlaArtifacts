package net.everla.everlaartifacts.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.api.client.util.RenderUtils;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Custom RenderType definitions for the mod's shader-based rendering.
 * <p>
 * Based on Avaritia's AvaritiaRenderTypes implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2026 Nova-Committee
 */
public class EverlaArtifactsRenderTypes {

    /**
     * RenderType for the cosmic starry sky effect on items.
     * Uses the cosmic shader with additive blending, equal depth testing
     * (so it draws on top of the existing item at the same depth),
     * and a custom texture state that isolates the block atlas on Sampler0.
     */
    public static final RenderType COSMIC = RenderType.create(
            new ResourceLocation(EverlaartifactsMod.MODID, "cosmic").toString(),
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            2097152,    // buffer size
            true,       // affectsCrumbling
            false,      // sortOnUpload
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            () -> EverlaArtifactsShaders.COSMIC_SHADER))
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderUtils.COSMIC_TEXTURE_ISOLATED)
                    .createCompositeState(true)
    );
}
