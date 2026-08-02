package net.everla.everlaartifacts.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerSprintMixin {

    /**
     * 拦截 hasEnoughFoodToStartSprinting()——原版中所有疾跑食物检查的汇聚点。
     * 若玩家穿着带有 Death Sprint 附魔的护腿，则直接返回 true 绕过饱食度限制。
     *
     * 影响范围：
     * - canStartSprinting() 的调用（双击W启动疾跑）
     * - aiStep() 中按住疾跑键的启动逻辑
     * - aiStep() 中低饱食度时停止疾跑的逻辑（line 687）
     */
    @Inject(method = "hasEnoughFoodToStartSprinting", at = @At("HEAD"), cancellable = true)
    private void onCheckFoodForSprint(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;

        ItemStack legs = self.getItemBySlot(EquipmentSlot.LEGS);
        int level = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.DEATH_SPRINT.get(), legs);

        if (level > 0) {
            cir.setReturnValue(true);
        }
    }
}
