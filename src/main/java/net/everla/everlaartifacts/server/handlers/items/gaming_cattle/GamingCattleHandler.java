package net.everla.everlaartifacts.server.handlers.items.gaming_cattle;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电竞牛头的效果处理（服务端）：基于佩戴者上报的当前 FPS 施加状态效果。
 * <p>
 * 低 FPS 获得增益，高 FPS 获得减益（阈值随 FPS 降低/升高叠加）：
 * <ul>
 *   <li>&lt;80 力量I，&lt;60 急迫II+速度II，&lt;40 生命恢复II，&lt;20 抗性提升II</li>
 *   <li>&gt;100 虚弱I，&gt;120 缓慢II+挖掘疲劳II，&gt;140 饥饿II，&gt;160 剧毒II</li>
 * </ul>
 * 装备判定：Curios 加载时检查饰品栏，同时始终检查原版头盔槽（无 Curios 时的兜底）。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GamingCattleHandler {

    /** 效果检查间隔（tick） */
    private static final int CHECK_INTERVAL = 10;
    /** 施加效果的基础时长（tick），远大于检查间隔，防止效果间断 */
    private static final int EFFECT_DURATION = 60;

    /** 记录本物品当前为各玩家施加的效果，仅移除自己施加的，避免误删外来效果 */
    private static final Map<UUID, Set<MobEffect>> GRANTED = new ConcurrentHashMap<>();

    private static boolean curiosLoaded = false;
    private static boolean curiosChecked = false;

    /** 懒加载并缓存 Curios 是否已加载 */
    private static boolean isCuriosLoaded() {
        if (!curiosChecked) {
            curiosChecked = true;
            try {
                ModList modList = ModList.get();
                curiosLoaded = modList != null && modList.isLoaded("curios");
            } catch (Exception e) {
                curiosLoaded = false;
            }
        }
        return curiosLoaded;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        if (!hasItemEquipped(player)) {
            clearEffects(player);
            return;
        }
        double fps = PerformanceMetrics.getPlayerFps(player.getUUID());
        applyEffects(player, fps);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            clearEffects(player);
        }
    }

    /** 根据当前 FPS 计算目标效果集（效果 → 等级放大器），并应用/移除 */
    private static void applyEffects(Player player, double fps) {
        Map<MobEffect, Integer> target = new HashMap<>();

        // 低 FPS 增益（阈值越低叠加越多）
        if (fps < 80.0) {
            target.put(MobEffects.DAMAGE_BOOST, 0);            // 力量 I
        }
        if (fps < 60.0) {
            target.put(MobEffects.DIG_SPEED, 1);               // 急迫 II
            target.put(MobEffects.MOVEMENT_SPEED, 1);          // 速度 II
        }
        if (fps < 40.0) {
            target.put(MobEffects.REGENERATION, 1);            // 生命恢复 II
        }
        if (fps < 20.0) {
            target.put(MobEffects.DAMAGE_RESISTANCE, 1);       // 抗性提升 II
        }

        // 高 FPS 减益（阈值越高叠加越多）
        if (fps > 100.0) {
            target.put(MobEffects.WEAKNESS, 0);                // 虚弱 I
        }
        if (fps > 120.0) {
            target.put(MobEffects.MOVEMENT_SLOWDOWN, 1);       // 缓慢 II
            target.put(MobEffects.DIG_SLOWDOWN, 1);            // 挖掘疲劳 II
        }
        if (fps > 140.0) {
            target.put(MobEffects.HUNGER, 1);                  // 饥饿 II
        }
        if (fps > 160.0) {
            target.put(MobEffects.POISON, 1);                  // 剧毒 II
        }

        UUID uuid = player.getUUID();
        Set<MobEffect> granted = GRANTED.computeIfAbsent(uuid, k -> new HashSet<>());

        // 移除不再生效的效果
        for (MobEffect effect : new HashSet<>(granted)) {
            if (!target.containsKey(effect)) {
                player.removeEffect(effect);
                granted.remove(effect);
            }
        }

        // 施加/刷新当前生效的效果
        for (Map.Entry<MobEffect, Integer> entry : target.entrySet()) {
            player.addEffect(new MobEffectInstance(entry.getKey(), EFFECT_DURATION,
                    entry.getValue(), false, true, true));
            granted.add(entry.getKey());
        }

        if (granted.isEmpty()) {
            GRANTED.remove(uuid);
        }
    }

    /** 清除本物品为玩家施加的所有效果 */
    private static void clearEffects(Player player) {
        Set<MobEffect> granted = GRANTED.remove(player.getUUID());
        if (granted != null) {
            for (MobEffect effect : granted) {
                player.removeEffect(effect);
            }
        }
    }

    /** 判定物品是否生效：原版头盔槽，或 Curios 加载时的饰品栏 */
    private static boolean hasItemEquipped(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == EverlaartifactsModItems.GAMING_CATTLE.get()) {
            return true;
        }
        if (isCuriosLoaded()) {
            return hasInCurios(player);
        }
        return false;
    }

    /** 仅当 Curios 加载时调用，避免引用不存在的类 */
    private static boolean hasInCurios(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(inventory -> inventory.isEquipped(EverlaartifactsModItems.GAMING_CATTLE.get()))
                .orElse(false);
    }
}
