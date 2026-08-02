package net.everla.everlaartifacts.server.handlers.enchantment;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles on-hit effects for the "A卡音质高" (AMD Sound Quality)
 * enchantment.
 * <p>
 * Triggered on {@link LivingHurtEvent} when the attacker is a player
 * holding a weapon with the enchantment:
 * <ul>
 *   <li><b>Player target</b>: Slowness V + Weakness V + Mining Fatigue V
 *       for 5 ticks.</li>
 *   <li><b>Non-player LivingEntity</b>: GenshinStart effect for 5 ticks.</li>
 *   <li><b>Warden</b>: additionally reduces HP by 30 via
 *       {@code setHealth}.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmdSoundQualityHandler {

    /** Debuff duration in ticks (5 ticks = 0.25 seconds). */
    private static final int DEBUFF_DURATION = 5;

    /** Extra health removed from Wardens. */
    private static final float WARDEN_BONUS_DAMAGE = 30.0F;

    @SubscribeEvent
    public static void onLivingHurt(final LivingHurtEvent event) {
        // Only process server-side
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // Attacker must be a player with the enchantment
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        int level = EnchantmentHelper.getTagEnchantmentLevel(
                EverlaartifactsModEnchantments.AMD_SOUND_QUALITY.get(), weapon);
        if (level <= 0) {
            return;
        }

        LivingEntity target = event.getEntity();

        if (target instanceof Player) {
            // PvP: triple debuff
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, 4)); // V
            target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, DEBUFF_DURATION, 4));           // V
            target.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN, DEBUFF_DURATION, 4));       // V
        } else {
            // PvE: GenshinStart & debuff
            target.addEffect(new MobEffectInstance(
                    EverlaartifactsModMobEffects.GENSHIN_START.get(),
                    DEBUFF_DURATION, 0));
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, 4)); // V
            target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, DEBUFF_DURATION, 4));           // V
            target.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN, DEBUFF_DURATION, 4));       // V
        }

        // Warden bonus: extra 30 HP removed via setHealth + particles
        if (target instanceof Warden warden) {
            float newHealth = warden.getHealth() - WARDEN_BONUS_DAMAGE;
            warden.setHealth(Math.max(1.0F, newHealth));

            if (warden.level() instanceof ServerLevel serverLevel) {
                double halfW = warden.getBbWidth() / 2.0;
                double halfH = warden.getBbHeight() / 2.0;
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        warden.getX(), warden.getY() + halfH, warden.getZ(),
                        20, halfW, halfH, halfW, 0.15);
            }
        }
    }
}
