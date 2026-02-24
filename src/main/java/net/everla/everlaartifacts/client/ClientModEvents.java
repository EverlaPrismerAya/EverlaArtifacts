package net.everla.everlaartifacts.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

import net.everla.everlaartifacts.client.renderer.AngolmoisDoomProjectileRenderer;
import net.everla.everlaartifacts.client.renderer.WatariNinaRenderer;
import net.everla.everlaartifacts.client.renderer.DanmakuRenderer;
import net.everla.everlaartifacts.client.model.Modelangolmois_doom;
import net.everla.everlaartifacts.client.model.ModelDanmaku;
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
            
            // 注册Watari Nina渲染器
            net.minecraft.client.renderer.entity.EntityRenderers.register(
                EverlaartifactsModEntities.WATARI_NINA.get(),
                WatariNinaRenderer::new
            );
            
            // 注册Danmaku渲染器
            net.minecraft.client.renderer.entity.EntityRenderers.register(
                EverlaartifactsModEntities.DANMAKU.get(),
                DanmakuRenderer::new
            );
        });
    }
    
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册Angolmois Doom模型层定义
        event.registerLayerDefinition(Modelangolmois_doom.LAYER_LOCATION, Modelangolmois_doom::createBodyLayer);
        
        // 注册Danmaku模型层定义
        event.registerLayerDefinition(ModelDanmaku.LAYER_LOCATION, ModelDanmaku::createBodyLayer);
    }
    
    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
    }
}