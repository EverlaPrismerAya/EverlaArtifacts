package net.everla.everlaartifacts.client.handlers.effects.american_style_cut;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class USADisplayHandler {
	
	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void onMobEffectAdded(MobEffectEvent.Added event) {
		if (event.getEffectInstance().getEffect() == EverlaartifactsModMobEffects.AMERICAN_STYLE_CUT_OVERLAY.get()) {
			Entity entity = event.getEntity();
			if (entity != null && entity.level() != null && !entity.level().isClientSide()) {
				// 播放 eagle_sound 音效
				SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(
					ResourceLocation.fromNamespaceAndPath("everlaartifacts", "eagle_sound")
				);
				entity.level().playSound(
					null,
					entity.getX(), 
					entity.getY(), 
					entity.getZ(), 
					soundEvent, 
					SoundSource.PLAYERS, 
					1.0F, 
					1.0F
				);
			}
		}
	}
	
	public static boolean handleUSADisplay(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(EverlaartifactsModMobEffects.AMERICAN_STYLE_CUT_OVERLAY.get()) ? _livEnt.getEffect(EverlaartifactsModMobEffects.AMERICAN_STYLE_CUT_OVERLAY.get()).getDuration() : 0) > 0) {
			return true;
		}
		return false;
	}
}