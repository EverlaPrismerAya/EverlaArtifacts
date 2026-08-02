package net.everla.everlaartifacts.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

/**
 * Holds the cosmic shader instance and its uniform variables.
 * Shader registration happens in {@link #onRegisterShaders(RegisterShadersEvent)}.
 * <p>
 * Based on Avaritia's AvaritiaShaders implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2026 Nova-Committee
 */
public class EverlaArtifactsShaders {

    /** UV coordinates for 10 cosmic sprites (u0, v0, u1, v1 each = 40 floats total). */
    public static final float[] COSMIC_UVS = new float[40];

    /** Cosmic sprite references, loaded from the block atlas after stitching. */
    public static TextureAtlasSprite[] COSMIC_SPRITES = new TextureAtlasSprite[10];

    public static ShaderInstance COSMIC_SHADER;

    public static Uniform cosmicTime;
    public static Uniform cosmicYaw;
    public static Uniform cosmicPitch;
    public static Uniform cosmicExternalScale;
    public static Uniform cosmicOpacity;
    public static Uniform cosmicUVs;

    /**
     * Called from ClientModEvents during RegisterShadersEvent.
     * Creates the cosmic shader instance and fetches all uniform handles.
     */
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(EverlaartifactsMod.MODID, "cosmic"),
                            DefaultVertexFormat.BLOCK),
                    shader -> {
                        COSMIC_SHADER = shader;
                        cosmicTime = COSMIC_SHADER.getUniform("time");
                        cosmicYaw = COSMIC_SHADER.getUniform("yaw");
                        cosmicPitch = COSMIC_SHADER.getUniform("pitch");
                        cosmicExternalScale = COSMIC_SHADER.getUniform("externalScale");
                        cosmicOpacity = COSMIC_SHADER.getUniform("opacity");
                        cosmicUVs = COSMIC_SHADER.getUniform("cosmicuvs");
                        COSMIC_SHADER.apply();
                    });
        } catch (Exception e) {
            EverlaartifactsMod.LOGGER.error("Failed to register cosmic shader", e);
        }
    }
}
