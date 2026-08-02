package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin into {@link Player#attack} to enforce the water/rain/lava condition
 * for the Impaling enchantment's damage bonus during melee trident attacks.
 * <p>
 * Intercepts two calls inside {@code Player.attack}:
 * <ol>
 *   <li>{@link Entity#hurt} — subtracts the impaling damage bonus when the
 *       target is not in water, rain, or lava.</li>
 *   <li>{@link Player#magicCrit} — suppresses the enchanted hit particles when
 *       impaling is the only active damage enchantment and the target is not in
 *       water, rain, or lava.</li>
 * </ol>
 *
 * @see ImpalingEnchantmentMixin
 * @see ThrownTridentImpalingMixin
 * @see EverlaArtifactsConfig#enhanceImpaling
 */
@Mixin(Player.class)
public abstract class PlayerAttackImpalingMixin {

    /**
     * Redirects the {@code Entity.hurt(DamageSource, float)} call inside
     * {@code Player.attack(Entity)} to check the target's fluid state.
     *
     * @param target the entity being attacked
     * @param source the damage source
     * @param amount the total damage amount including enchantment bonuses
     * @return the result of {@code target.hurt(source, modifiedAmount)}
     */
    @Redirect(method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean everlaartifacts$checkImpalingOnMelee(
            Entity target, DamageSource source, float amount) {
        if (!EverlaArtifactsConfig.isEnhanceImpaling()) {
            return target.hurt(source, amount);
        }

        // Only applicable when hitting a LivingEntity
        if (target instanceof LivingEntity livingTarget) {
            // If the target IS in water, rain, bubble column, or lava,
            // the impaling bonus correctly applies — no modification needed.
            if (!livingTarget.isInWaterRainOrBubble() && !livingTarget.isInLava()) {
                // Target is NOT in a valid fluid — subtract the impaling bonus.
                Player self = (Player) (Object) this;
                ItemStack weapon = self.getMainHandItem();
                int impalingLevel = weapon.getEnchantmentLevel(Enchantments.IMPALING);

                if (impalingLevel > 0) {
                    float impalingBonus = impalingLevel == 1
                            ? 2.0F
                            : 2.0F + (impalingLevel - 1) * 2.5F;
                    amount = Math.max(0.0F, amount - impalingBonus);
                }
            }
        }

        return target.hurt(source, amount);
    }

    /**
     * Redirects the {@code Player.magicCrit(Entity)} call inside
     * {@code Player.attack(Entity)} to suppress enchanted hit particles when
     * the Impaling enchantment's damage bonus is not actually active.
     * <p>
     * Vanilla calls {@code magicCrit} whenever {@code getDamageBonus} returns
     * a positive value. Since {@link ImpalingEnchantmentMixin} makes impaling
     * always return a bonus, the particle would always play even when the
     * target is not in water/rain/lava. This redirect suppresses the particle
     * in that specific case.
     * <p>
     * If the weapon has other damage-boosting enchantments (Sharpness, Smite,
     * Bane of Arthropods), the particle is still allowed through.
     *
     * @param self   the attacking player (receiver of the magicCrit call)
     * @param target the entity being attacked
     */
    @Redirect(method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;magicCrit(Lnet/minecraft/world/entity/Entity;)V"))
    private void everlaartifacts$suppressMagicCritOnMelee(
            Player self, Entity target) {
        if (!EverlaArtifactsConfig.isEnhanceImpaling()) {
            self.magicCrit(target);
            return;
        }

        // Check whether the target is in a valid fluid for impaling
        boolean targetInFluid = target instanceof LivingEntity livingTarget
                && (livingTarget.isInWaterRainOrBubble() || livingTarget.isInLava());

        if (targetInFluid) {
            // Target is in fluid — impaling applies, particles are correct
            self.magicCrit(target);
            return;
        }

        // Target NOT in fluid — check if impaling is the only damage enchantment
        ItemStack weapon = self.getMainHandItem();
        int impalingLevel = weapon.getEnchantmentLevel(Enchantments.IMPALING);

        if (impalingLevel > 0) {
            // Check for other damage-boosting enchantments
            boolean hasOtherDamageEnchant = weapon.getEnchantmentLevel(Enchantments.SHARPNESS) > 0
                    || weapon.getEnchantmentLevel(Enchantments.SMITE) > 0
                    || weapon.getEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS) > 0;

            if (!hasOtherDamageEnchant) {
                // Impaling is the only damage enchantment and target is not in
                // fluid — suppress the enchanted hit particles entirely.
                return;
            }
        }

        // Allow particles through (no impaling, or other enchantments present)
        self.magicCrit(target);
    }
}
