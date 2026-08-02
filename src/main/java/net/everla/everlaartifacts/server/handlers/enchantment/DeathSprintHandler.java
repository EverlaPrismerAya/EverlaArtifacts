package net.everla.everlaartifacts.server.handlers.enchantment;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class DeathSprintHandler {

    private static final String LAST_DAMAGE_TICK_KEY = "DeathSprintLastDamageTick";
    private static final String DEATH_FLAG_KEY = "DeathSprintKilled";
    private static final int DAMAGE_INTERVAL_TICKS = 40; // 2秒
    private static final int HUNGER_THRESHOLD = 6; // 3格饱食度
    private static final float DAMAGE_AMOUNT = 1.0F;

    private static Holder<DamageType> cachedDestinyKillType;

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

        // 饥饿状态下疾跑时持续扣血（每2秒1点）
        if (player.isSprinting() && player.getFoodData().getFoodLevel() < HUNGER_THRESHOLD) {
            long currentTick = player.level().getGameTime();
            long lastDamageTick = player.getPersistentData().getLong(LAST_DAMAGE_TICK_KEY);

            if (currentTick - lastDamageTick >= DAMAGE_INTERVAL_TICKS) {
                // 标记本次伤害来自 Death Sprint，用于死亡检测
                player.getPersistentData().putBoolean(DEATH_FLAG_KEY, true);
                player.hurt(getDestinyKillDamageSource(player), DAMAGE_AMOUNT);
                player.getPersistentData().putBoolean(DEATH_FLAG_KEY, false);
                player.getPersistentData().putLong(LAST_DAMAGE_TICK_KEY, currentTick);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 仅当标记存在时触发——说明死于 Death Sprint 的扣血
        if (!player.getPersistentData().getBoolean(DEATH_FLAG_KEY)) return;

        // 清理标记
        player.getPersistentData().remove(DEATH_FLAG_KEY);

        // 授予自定义进度
        Advancement advancement = player.server.getAdvancements()
                .getAdvancement(ResourceLocation.fromNamespaceAndPath(
                        "everlaartifacts", "adventure/death_sprint_death"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "death_by_destiny_kill");
        }
    }

    private static DamageSource getDestinyKillDamageSource(Player player) {
        if (cachedDestinyKillType == null) {
            cachedDestinyKillType = player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolder(ResourceKey.create(Registries.DAMAGE_TYPE,
                            ResourceLocation.fromNamespaceAndPath("everlaartifacts", "destiny_kill")))
                    .orElseThrow();
        }
        return new DamageSource(cachedDestinyKillType);
    }
}
