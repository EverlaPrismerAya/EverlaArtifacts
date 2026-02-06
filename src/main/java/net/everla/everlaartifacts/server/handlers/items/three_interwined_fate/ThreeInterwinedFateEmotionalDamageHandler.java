package net.everla.everlaartifacts.server.handlers.items.three_interwined_fate;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

@Mod.EventBusSubscriber
public class ThreeInterwinedFateEmotionalDamageHandler {
    
    public static void handleThreeInterwinedFateEmotionalDamage(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null)
            return;
        entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 20);
        if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:emotional_damage")), SoundSource.NEUTRAL, 1, 1);
            } else {
                _level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:emotional_damage")), SoundSource.NEUTRAL, 1, 1, false);
            }
        }
        if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
            _entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 4800, 3, false, true));
        if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
            _entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2400, 1, false, true));
        if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
            _entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 7200, 0, false, true));
        if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
            _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 7200, 0, false, true));
    }
}