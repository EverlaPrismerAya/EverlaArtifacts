package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin into {@link ThrownTrident#onHitEntity} to enforce the water/rain/lava
 * condition for the Impaling enchantment's damage bonus during thrown trident
 * attacks.
 * <p>
 * Intercepts the {@link Entity#hurt} call inside
 * {@code ThrownTrident.onHitEntity} where both the trident projectile and
 * the target entity are directly accessible. If the target is <b>not</b> in
 * water, rain, or lava, the impaling portion of the damage is subtracted
 * before the {@code hurt} call proceeds.
 *
 * @see ImpalingEnchantmentMixin
 * @see PlayerAttackImpalingMixin
 * @see EverlaArtifactsConfig#enhanceImpaling
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentImpalingMixin {

    /**
     * Redirects the {@code Entity.hurt(DamageSource, float)} call inside
     * {@code ThrownTrident.onHitEntity(EntityHitResult)} to check the target's
     * fluid state.
     *
     * @param target the entity being hit by the trident
     * @param source the damage source
     * @param amount the total damage amount including enchantment bonuses
     * @return the result of {@code target.hurt(source, modifiedAmount)}
     */
    @Redirect(method = "onHitEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean everlaartifacts$checkImpalingOnThrown(
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
                ThrownTrident self = (ThrownTrident) (Object) this;
                ItemStack tridentItem = ((AbstractArrowAccessor) self)
                        .everlaartifacts$invokeGetPickupItem();
                int impalingLevel = tridentItem
                        .getEnchantmentLevel(Enchantments.IMPALING);

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
}
