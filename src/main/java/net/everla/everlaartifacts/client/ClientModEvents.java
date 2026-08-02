package net.everla.everlaartifacts.client;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.client.model.loader.CosmicModelLoader;
import net.everla.everlaartifacts.client.shader.EverlaArtifactsShaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

import net.everla.everlaartifacts.client.renderer.AngolmoisDoomProjectileRenderer;
import net.everla.everlaartifacts.client.model.Modelangolmois_doom;
import net.everla.everlaartifacts.init.EverlaartifactsModEntities;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Register Angolmois Doom renderer
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.entity.EntityRenderers.register(
                EverlaartifactsModEntities.ANGOLMOIS_DOOM_PROJECTILE.get(),
                AngolmoisDoomProjectileRenderer::new
            );
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Register Angolmois Doom model layer definition
        event.registerLayerDefinition(Modelangolmois_doom.LAYER_LOCATION, Modelangolmois_doom::createBodyLayer);
    }

    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
    }

    // ---- Cosmic Shader & Model Loader Registration ----

    /**
     * Registers the cosmic shader for starry sky item rendering.
     */
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        EverlaArtifactsShaders.onRegisterShaders(event);
    }

    /**
     * Registers the "everlaartifacts:cosmic" geometry model loader.
     * This allows model JSON files to specify {@code "loader": "everlaartifacts:cosmic"}
     * to trigger the cosmic starry sky rendering pipeline.
     */
    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("mcreator_overlay", CosmicModelLoader.INSTANCE);
    }

    /**
     * After the block atlas texture is stitched, fetch the cosmic star sprite
     * (mcreator.png) and compute its UV coordinates. These UVs are passed to
     * the cosmic fragment shader so it can sample the starry sky icon.
     * <p>
     * All 10 cosmic sprite slots use the same mcreator texture.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            // Use mcreator.png as the starry sky icon for all 10 cosmic slots
            for (int i = 0; i < EverlaArtifactsShaders.COSMIC_SPRITES.length; i++) {
                EverlaArtifactsShaders.COSMIC_SPRITES[i] = event.getAtlas()
                        .getSprite(new ResourceLocation(EverlaartifactsMod.MODID,
                                "misc/mcreator/mcreator"));
                EverlaArtifactsShaders.COSMIC_UVS[i * 4] = EverlaArtifactsShaders.COSMIC_SPRITES[i].getU0();
                EverlaArtifactsShaders.COSMIC_UVS[i * 4 + 1] = EverlaArtifactsShaders.COSMIC_SPRITES[i].getV0();
                EverlaArtifactsShaders.COSMIC_UVS[i * 4 + 2] = EverlaArtifactsShaders.COSMIC_SPRITES[i].getU1();
                EverlaArtifactsShaders.COSMIC_UVS[i * 4 + 3] = EverlaArtifactsShaders.COSMIC_SPRITES[i].getV1();
            }
        }
    }
}
