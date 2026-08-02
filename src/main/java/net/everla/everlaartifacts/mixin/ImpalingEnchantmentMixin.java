package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.TridentImpalerEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for vanilla {@link TridentImpalerEnchantment} to modify its damage
 * formula when the {@code enhanceImpaling} config option is enabled.
 * <p>
 * Changes:
 * <ul>
 *   <li>Damage bonus formula: level 1 → +2.0, each additional level → +2.5</li>
 *   <li>Removes MobType.WATER restriction — bonus is returned for all mob types.
 *       The actual water/rain/lava entity-state check is handled by
 *       {@link PlayerAttackImpalingMixin} and {@link ThrownTridentImpalingMixin}.</li>
 * </ul>
 * <p>
 * Formula table:
 * <pre>
 *   Level 1: 2.0
 *   Level 2: 4.5  (2.0 + 2.5)
 *   Level 3: 7.0  (2.0 + 5.0)
 *   Level 4: 9.5  (2.0 + 7.5)
 *   Level 5: 12.0 (2.0 + 10.0)
 * </pre>
 *
 * @see EverlaArtifactsConfig#enhanceImpaling
 */
@Mixin(TridentImpalerEnchantment.class)
public abstract class ImpalingEnchantmentMixin {

    /**
     * Intercepts {@code getDamageBonus} to replace the vanilla formula.
     * <p>
     * Vanilla: {@code level * 2.5F} (only for {@link MobType#WATER}).
     * <br>
     * New: {@code level == 1 ? 2.0F : 2.0F + (level - 1) * 2.5F} (all mob types).
     * <p>
     * Only active when {@link EverlaArtifactsConfig#enhanceImpaling} is {@code true}.
     */
    @Inject(method = "getDamageBonus", at = @At("HEAD"), cancellable = true)
    private void everlaartifacts$modifyImpalingDamageBonus(
            int level, MobType mobType, CallbackInfoReturnable<Float> cir) {
        if (!EverlaArtifactsConfig.isEnhanceImpaling()) {
            return; // config disabled — use vanilla behavior
        }
        float bonus = level == 1 ? 2.0F : 2.0F + (level - 1) * 2.5F;
        cir.setReturnValue(bonus);
    }
}
