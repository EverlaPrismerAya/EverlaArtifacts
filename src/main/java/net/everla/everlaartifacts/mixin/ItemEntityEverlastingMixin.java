package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.common.handlers.data_driven.EverlastingItemHandler;
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
 * Mixin to make everlasting-tagged items immune to:
 * <ul>
 *   <li><b>Explosions</b> — like Nether Stars, item entities carrying
 *       everlasting items survive blasts</li>
 *   <li><b>Fire & Lava</b> — like Netherite, everlasting items don't burn</li>
 * </ul>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityEverlastingMixin {

    @Shadow
    public abstract ItemStack getItem();

    /**
     * Makes everlasting items immune to explosion damage.
     * Mimics the vanilla nether star check.
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void everlaartifacts$cancelExplosionDamage(DamageSource source, float amount,
                                                        CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = this.getItem();
        if (EverlastingItemHandler.isEverlasting(stack) && source.is(DamageTypeTags.IS_EXPLOSION)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Makes everlasting items immune to fire and lava.
     * Equivalent to the {@code isFireResistant()} check on the Item.
     */
    @Inject(method = "fireImmune", at = @At("TAIL"), cancellable = true)
    private void everlaartifacts$makeFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            ItemStack stack = this.getItem();
            if (EverlastingItemHandler.isEverlasting(stack)) {
                cir.setReturnValue(true);
            }
        }
    }
}
