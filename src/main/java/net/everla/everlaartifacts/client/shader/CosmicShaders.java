package net.everla.everlaartifacts.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

public class CosmicShaders {
    public static final ResourceLocation COSMIC_ATLAS = new ResourceLocation(EverlaartifactsMod.MODID, "cosmic");
    public static final float[] COSMIC_UVS = new float[40];
    public static final TextureAtlasSprite[] COSMIC_SPRITES = new TextureAtlasSprite[10];

    public static ShaderInstance COSMIC_SHADER;
    public static Uniform cosmicTime;
    public static Uniform cosmicYaw;
    public static Uniform cosmicPitch;
    public static Uniform cosmicExternalScale;
    public static Uniform cosmicOpacity;
    public static Uniform cosmicUVs;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        EverlaartifactsMod.LOGGER.info("[CosmicShaders] Registering cosmic shader...");
        try {
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(), new ResourceLocation(EverlaartifactsMod.MODID, "cosmic"), DefaultVertexFormat.BLOCK),
                shader -> {
                    COSMIC_SHADER = shader;
                    cosmicTime = shader.getUniform("time");
                    cosmicYaw = shader.getUniform("yaw");
                    cosmicPitch = shader.getUniform("pitch");
                    cosmicExternalScale = shader.getUniform("externalScale");
                    cosmicOpacity = shader.getUniform("opacity");
                    cosmicUVs = shader.getUniform("cosmicuvs");
                    EverlaartifactsMod.LOGGER.info("[CosmicShaders] Shader registered successfully!");
                }
            );
        } catch (Exception e) {
            EverlaartifactsMod.LOGGER.error("[CosmicShaders] Failed to register cosmic shader", e);
        }
    }

    public static ShaderInstance getCosmicShader() {
        updateUniforms();
        return COSMIC_SHADER;
    }

    public static void updateUniforms() {
        if (COSMIC_SHADER == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        ensureCosmicUvs(mc);

        float yaw = 0.0f;
        float pitch = 0.0f;
        float scale = 1.0f;

        if (mc.screen != null) {
            scale = 100.0f;
        } else if (mc.player != null) {
            yaw = (float) (mc.player.getYRot() * 2.0f * Math.PI / 360.0);
            pitch = (float) (-(mc.player.getXRot() * 2.0f * Math.PI / 360.0));
        }

        if (cosmicTime != null) {
            cosmicTime.set(mc.level.getGameTime() % Integer.MAX_VALUE);
        }
        if (cosmicYaw != null) {
            cosmicYaw.set(yaw);
        }
        if (cosmicPitch != null) {
            cosmicPitch.set(pitch);
        }
        if (cosmicExternalScale != null) {
            cosmicExternalScale.set(scale);
        }
        if (cosmicOpacity != null) {
            cosmicOpacity.set(1.0f);
        }
        if (cosmicUVs != null) {
            cosmicUVs.set(COSMIC_UVS);
        }
    }

    private static void ensureCosmicUvs(Minecraft mc) {
        if (COSMIC_SPRITES[0] != null) {
            return;
        }
        for (int i = 0; i < COSMIC_SPRITES.length; i++) {
            ResourceLocation loc = new ResourceLocation(EverlaartifactsMod.MODID, "misc/cosmic/cosmic_" + i);
            TextureAtlasSprite sprite = mc.getTextureAtlas(COSMIC_ATLAS).apply(loc);
            COSMIC_SPRITES[i] = sprite;
            COSMIC_UVS[i * 4] = sprite.getU0();
            COSMIC_UVS[i * 4 + 1] = sprite.getV0();
            COSMIC_UVS[i * 4 + 2] = sprite.getU1();
            COSMIC_UVS[i * 4 + 3] = sprite.getV1();
        }
    }
}
