package net.everla.everlaartifacts.client;

import net.minecraftforge.api.distmarker.Dist;
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
        // 注册Angolmois Doom渲染器
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.entity.EntityRenderers.register(
                EverlaartifactsModEntities.ANGOLMOIS_DOOM_PROJECTILE.get(), 
                AngolmoisDoomProjectileRenderer::new
            );
        });
    }
    
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册Angolmois Doom模型层定义
        event.registerLayerDefinition(Modelangolmois_doom.LAYER_LOCATION, Modelangolmois_doom::createBodyLayer);
    }
    
    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
    }
}