
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.everla.everlaartifacts.client.model.Modelangolmois_doom;
import net.everla.everlaartifacts.client.model.ModelPlayerModel;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class EverlaartifactsModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelangolmois_doom.LAYER_LOCATION, Modelangolmois_doom::createBodyLayer);
		event.registerLayerDefinition(ModelPlayerModel.LAYER_LOCATION, ModelPlayerModel::createBodyLayer);
	}
}
