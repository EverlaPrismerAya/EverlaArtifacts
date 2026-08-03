package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让「自由升级」附带隐藏的绑定诅咒效果。
 * <p>
 * 原版 1.20.1 中，生存背包铠甲槽位的 {@code mayPickup} 通过
 * {@link EnchantmentHelper#hasBindingCurse(ItemStack)} 判断物品是否被绑定——
 * 绑定物品在生存模式下无法从铠甲槽取下，仅创造模式可以。该检查是绑定的唯一出口，
 * 因此在 {@code hasBindingCurse} 的 HEAD 注入：当物品带有「自由升级」附魔时，
 * 同样视作绑定诅咒，从而复刻原版「绑定诅咒」的行为。
 * <p>
 * 该行为不写入附魔描述（隐藏诅咒）。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperBindingMixin {

    @Inject(method = "hasBindingCurse", at = @At("HEAD"), cancellable = true)
    private static void everlaartifacts$escalationOfFreedomBinds(
            ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (EnchantmentHelper.getTagEnchantmentLevel(
                EverlaartifactsModEnchantments.ESCALATION_OF_FREEDOM.get(), stack) > 0) {
            cir.setReturnValue(true);
        }
    }
}
