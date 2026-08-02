package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.common.handlers.enchantment.LayeredBufferHandler;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link LivingEntity#hurt} that provides Layered Buffer
 * protection at the <b>highest possible priority</b>.
 * <p>
 * By injecting at the {@code HEAD} of {@code hurt()}, this runs
 * <b>before</b> any Forge event ({@code LivingHurtEvent},
 * {@code LivingAttackEvent}, etc.), armour calculation, or potion
 * effect reduction. This guarantees that the Layered Buffer always
 * gets the first opportunity to absorb incoming damage — other mods
 * cannot bypass it through custom damage paths.
 * <p>
 * The {@link LayeredBufferHandler} Forge event handlers remain as a
 * secondary safety net for death prevention and UI display.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLayeredBufferMixin {

    /**
     * Intercepts {@code hurt(DamageSource, float)} at {@code HEAD} to apply
     * the Layered Buffer enchantment logic before any other processing.
     * <p>
     * If the buffer successfully absorbs the hit, the method is cancelled
     * (returns {@code false}) without any vanilla or Forge processing.
     * If the buffer is exhausted, the enchantment is stripped and the
     * damage proceeds normally.
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void everlaartifacts$layeredBufferAbsorb(
            DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only players are protected
        if (!(self instanceof Player player)) {
            return;
        }

        // Don't protect creative-mode players
        if (player.isCreative()) {
            return;
        }

        // Check for Layered Buffer on the chest slot
        ItemStack chestArmour = player.getItemBySlot(EquipmentSlot.CHEST);
        int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.LAYERED_BUFFER.get(), chestArmour);
        if (enchantLevel <= 0) {
            return;
        }

        // Increment the damage counter and compute the threshold
        int currentDamage = LayeredBufferHandler.getDamageLayers(chestArmour) + 1;
        LayeredBufferHandler.setDamageLayers(chestArmour, currentDamage);

        int threshold = LayeredBufferHandler.calculateThreshold(enchantLevel);

        if (currentDamage >= threshold) {
            // Buffer exhausted — strip enchantment and let damage through
            LayeredBufferHandler.removeEnchantmentAndCleanup(chestArmour, player);
            return;
        }

        // Buffer active — cancel all damage processing
        cir.setReturnValue(false);
        player.invulnerableTime = 0;

        // Visual feedback
        if (player.level() instanceof ServerLevel serverLevel) {
            double halfW = player.getBbWidth() / 2.0;
            double halfH = player.getBbHeight() / 2.0;
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + halfH, player.getZ(),
                    10, halfW, halfH, halfW, 0.1);
        }
    }
}
