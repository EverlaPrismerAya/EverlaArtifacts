package net.everla.everlaartifacts.client;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.client.model.loader.CosmicModelLoader;
import net.everla.everlaartifacts.client.shader.CosmicShaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = EverlaartifactsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CosmicClientEvents {
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        CosmicShaders.onRegisterShaders(event);
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        EverlaartifactsMod.LOGGER.info("[CosmicClientEvents] Registering cosmic geometry loader");
        event.register("cosmic", CosmicModelLoader.INSTANCE);
        EverlaartifactsMod.LOGGER.info("[CosmicClientEvents] Geometry loader registered");
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModelEvent(ModelEvent.RegisterAdditional event) {
        EverlaartifactsMod.LOGGER.info("[CosmicClientEvents] Registering cosmic atlas");
        // 注册 cosmic atlas，这样纹理才能被正确加载
        event.register(new ResourceLocation(EverlaartifactsMod.MODID, "cosmic"));
        EverlaartifactsMod.LOGGER.info("[CosmicClientEvents] Cosmic atlas registered");
    }
}
