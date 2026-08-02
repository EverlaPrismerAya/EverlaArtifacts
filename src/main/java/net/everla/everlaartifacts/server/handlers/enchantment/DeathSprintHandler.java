package net.everla.everlaartifacts.server.handlers.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class DeathSprintHandler {

    private static final String LAST_DAMAGE_TICK_KEY = "DeathSprintLastDamageTick";
    private static final int DAMAGE_INTERVAL_TICKS = 40; // 2秒
    private static final int HUNGER_THRESHOLD = 6; // 3格饱食度
    private static final float DAMAGE_AMOUNT = 1.0F;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide()) return;

        // 检查腿部护甲是否带有 Death Sprint 附魔
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        int level = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.DEATH_SPRINT.get(), legs);
        if (level <= 0) return;

        // 始终允许疾跑 — 绕过原版饱食度限制
        // 通过对比当前坐标与上一 tick 坐标来判断玩家是否在前进
        Vec3 delta = player.position().subtract(
                new Vec3(player.xOld, player.yOld, player.zOld));
        double horizontalSpeed = delta.x * delta.x + delta.z * delta.z;

        // 如果玩家正在水平移动且未在水中/骑乘，强制开启疾跑
        if (horizontalSpeed > 0.0001 && !player.isInWater() && !player.isPassenger()) {
            player.setSprinting(true);
        }

        // 饥饿状态下疾跑时持续扣血
        if (player.isSprinting() && player.getFoodData().getFoodLevel() < HUNGER_THRESHOLD) {
            long currentTick = player.level().getGameTime();
            long lastDamageTick = player.getPersistentData().getLong(LAST_DAMAGE_TICK_KEY);

            if (currentTick - lastDamageTick >= DAMAGE_INTERVAL_TICKS) {
                player.hurt(player.damageSources().generic(), DAMAGE_AMOUNT);
                player.getPersistentData().putLong(LAST_DAMAGE_TICK_KEY, currentTick);
            }
        }
    }
}