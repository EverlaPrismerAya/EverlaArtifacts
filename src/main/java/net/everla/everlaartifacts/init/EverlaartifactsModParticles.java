
package net.everla.everlaartifacts.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.everla.everlaartifacts.client.particle.GoldButterflyParticle;
import net.everla.everlaartifacts.client.particle.FireButterflyParticle;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EverlaartifactsModParticles {
	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(EverlaartifactsModParticleTypes.GOLD_BUTTERFLY.get(), GoldButterflyParticle::provider);
		event.registerSpriteSet(EverlaartifactsModParticleTypes.FIRE_BUTTERFLY.get(), FireButterflyParticle::provider);
	}
}
