package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.common.handlers.enchantment.QuickChargeHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Applies the hit effects of Quick Charge &gt; 5 crossbow arrows.
 * <p>
 * Arrows fired by such crossbows are tagged in
 * {@link CrossbowItemQuickChargeMixin#everlaartifacts$tagAutoFireArrow} with the
 * {@code everlaartifacts:auto_fire_arrow} flag and the per-level damage
 * reduction factor. When a tagged arrow lands, this mixin:
 * <ul>
 *   <li><b>Bypasses the target's i-frames</b> — sets {@code invulnerableTime}
 *       to 0 just before the {@code hurt} call so rapid-fire hits all register
 *       instead of being absorbed by the victim's invulnerability window.</li>
 *   <li><b>Reduces the arrow's damage</b> by 12% per Quick Charge level above
 *       5, capped at 48%.</li>
 * </ul>
 * <p>
 * The check reads the arrow's own persistent data, so it does not depend on
 * whether the owner still holds the crossbow when the arrow lands.
 *
 * @see CrossbowItemQuickChargeMixin
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowQuickChargeMixin {

    /**
     * Redirects the {@code entity.hurt(...)} call inside
     * {@code AbstractArrow.onHitEntity}. For tagged arrows, clears the target's
     * invulnerability timer and applies the damage reduction before delegating
     * to the vanilla {@code hurt}.
     */
    @Redirect(method = "onHitEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean everlaartifacts$autoFireHit(Entity entity, DamageSource source, float damage) {
        // `this` is the arrow at runtime; the mixin class has no supertype so cast via Object.
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.getPersistentData().getBoolean(QuickChargeHandler.TAG_AUTO_FIRE_ARROW)) {
            if (entity instanceof LivingEntity living) {
                // Bypass invulnerability frames so every hit of the volley lands.
                living.invulnerableTime = 0;
            }
            float reduction = arrow.getPersistentData()
                    .getFloat(QuickChargeHandler.TAG_DAMAGE_REDUCTION);
            damage *= (1.0F - reduction);
        }
        return entity.hurt(source, damage);
    }
}
