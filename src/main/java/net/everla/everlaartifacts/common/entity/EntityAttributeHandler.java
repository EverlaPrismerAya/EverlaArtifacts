package net.everla.everlaartifacts.common.entity;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModEntities;
import net.everla.everlaartifacts.common.entity.bosses.watari_nina.WatariNinaEntity;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityAttributeHandler {
    
    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(EverlaartifactsModEntities.WATARI_NINA.get(), WatariNinaEntity.createAttributes().build());
    }
}