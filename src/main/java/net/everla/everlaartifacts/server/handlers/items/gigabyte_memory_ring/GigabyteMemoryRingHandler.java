package net.everla.everlaartifacts.server.handlers.items.gigabyte_memory_ring;

import net.everla.everlaartifacts.common.item.GigabyteMemoryRingItem;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/**
 * 千兆内存之戒的效果处理（服务端）：
 * <ul>
 *   <li>每 8GB 物理内存 → +2.5% 伤害</li>
 *   <li>每 1GB 显存 → +0.5% 伤害 与 +2% 攻击速度</li>
 * </ul>
 * Curios API 加载时戒指佩戴于饰品栏；未加载时放置于副手生效。
 * <p>
 * 伤害在 {@link LivingDamageEvent} 中按倍数放大；攻击速度通过对
 * {@link Attributes#ATTACK_SPEED} 添加 {@link AttributeModifier} 实现。
 * 加成基于佩戴者上报的硬件数据（见 {@link PerformanceMetrics}），
 * 未获取到数据时按 0 处理（无加成）。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GigabyteMemoryRingHandler {

    /** 攻击速度修饰符固定 UUID，确保可被可靠移除 */
    private static final UUID ATTACK_SPEED_UUID =
            UUID.fromString("8d3f2a11-4b5c-4d6e-8f70-1a2b3c4d5e6f");

    private static final String ATTACK_SPEED_NAME = "gigabyte_memory_ring_speed";

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
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player attacker)) {
            return;
        }
        if (!hasRingEquipped(attacker)) {
            return;
        }

        int ramMB = PerformanceMetrics.getPlayerPhysicalMemoryMB(attacker);
        int vramMB = PerformanceMetrics.getPlayerVramMB(attacker);

        double damageMultiplier = GigabyteMemoryRingItem.calculateDamageMultiplier(ramMB, vramMB);

        event.setAmount((float) (event.getAmount() * damageMultiplier));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        // 每 20 tick（1 秒）检查一次，覆盖饰品栏/副手装备变化
        if (player.tickCount % 20 != 0) {
            return;
        }
        updateAttackSpeed(player);
    }

    /**
     * 判定戒指是否生效：Curios 加载时检查饰品栏，未加载时检查副手。
     */
    private static boolean hasRingEquipped(Player player) {
        if (isCuriosLoaded()) {
            return hasRingInCurios(player);
        }
        return player.getOffhandItem().getItem() == EverlaartifactsModItems.GIGABYTE_MEMORY_RING.get();
    }

    /** 仅当 Curios 加载时调用，避免引用不存在的类 */
    private static boolean hasRingInCurios(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(inventory -> inventory.isEquipped(EverlaartifactsModItems.GIGABYTE_MEMORY_RING.get()))
                .orElse(false);
    }

    private static void updateAttackSpeed(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute == null) {
            return;
        }
        if (!hasRingEquipped(player)) {
            attribute.removeModifier(ATTACK_SPEED_UUID);
            return;
        }
        int vramMB = PerformanceMetrics.getPlayerVramMB(player);
        if (vramMB <= 0) {
            attribute.removeModifier(ATTACK_SPEED_UUID);
            return;
        }
        double bonus = GigabyteMemoryRingItem.calculateAttackSpeedBonus(vramMB);
        attribute.removeModifier(ATTACK_SPEED_UUID);
        attribute.addTransientModifier(new AttributeModifier(
                ATTACK_SPEED_UUID, ATTACK_SPEED_NAME, bonus,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
