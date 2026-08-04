package net.everla.everlatweaker.mixin;

import net.everla.everlatweaker.common.handlers.data_driven.ProtectiveTagsHandler;
import net.everla.everlatweaker.common.handlers.data_driven.EverlastingItemHandler;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make tagged items immune to damage:
 * <ul>
 *   <li><b>Explosions</b> — everlasting-tagged items and items carrying the
 *       {@code everlatweaker:explosion_resistant} tag survive blasts, like
 *       Nether Stars</li>
 *   <li><b>Fire & Lava</b> — everlasting-tagged items and items carrying the
 *       {@code everlatweaker:fire_resistant} tag don't burn, like Netherite</li>
 * </ul>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityEverlastingMixin {

    @Shadow
    public abstract ItemStack getItem();

    /**
     * Makes explosion-resistant and everlasting items immune to explosion damage.
     * Mimics the vanilla nether star check.
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void everlaartifacts$cancelExplosionDamage(DamageSource source, float amount,
                                                        CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = this.getItem();
        if (source.is(DamageTypeTags.IS_EXPLOSION)
                && (EverlastingItemHandler.isEverlasting(stack) || ProtectiveTagsHandler.isExplosionResistant(stack))) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Makes fire-resistant and everlasting items immune to fire and lava.
     * Equivalent to the {@code isFireResistant()} check on the Item.
     */
    @Inject(method = "fireImmune", at = @At("TAIL"), cancellable = true)
    private void everlaartifacts$makeFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            ItemStack stack = this.getItem();
            if (EverlastingItemHandler.isEverlasting(stack) || ProtectiveTagsHandler.isFireResistant(stack)) {
                cir.setReturnValue(true);
            }
        }
    }
}
