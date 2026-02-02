package net.everla.everlaartifacts.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // 在这里可以进行客户端相关的初始化
    }
    
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册自定义渲染层定义
    }
    
    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        // 新的BloodBlossom渲染系统使用网络包，不需要添加渲染层
    }
}