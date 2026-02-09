package net.everla.everlaartifacts.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.everla.everlaartifacts.EverlaartifactsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class CosmicRenderTypes {
    private static final RenderStateShard.DepthTestStateShard COSMIC_DEPTH_TEST =
        new RenderStateShard.DepthTestStateShard("cosmic_equal_depth", GL11.GL_EQUAL);
    private static final RenderStateShard.LightmapStateShard COSMIC_LIGHTMAP =
        new RenderStateShard.LightmapStateShard(true);
    private static final RenderStateShard.TransparencyStateShard COSMIC_TRANSPARENCY =
        new RenderStateShard.TransparencyStateShard(
            "cosmic_translucent",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
        );

    public static final RenderType COSMIC = RenderType.create(
        EverlaartifactsMod.MODID + ":cosmic",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        2097152,
        true,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(CosmicShaders::getCosmicShader))
            .setDepthTestState(COSMIC_DEPTH_TEST)
            .setLightmapState(COSMIC_LIGHTMAP)
            .setTransparencyState(COSMIC_TRANSPARENCY)
            .setTextureState(new RenderStateShard.TextureStateShard(CosmicShaders.COSMIC_ATLAS, false, false))
            .createCompositeState(true)
    );

    private CosmicRenderTypes() {
    }
}
