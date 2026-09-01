package net.everla.everlaartifacts.server.handlers.items.commoner_necklace;

import net.everla.everlaartifacts.common.item.CommonerNecklaceItem;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.server.level.ServerPlayer;
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
 * 平民项链的效果处理（服务端）：
 * <ul>
 *   <li>佩戴者攻击力随显卡显存变化（≤4G 最高 +10%，≥16G 最低 -10%，锚点间线性插值）</li>
 *   <li>显存低于 8G 时受到的伤害降低 10%；高于 10G 时受到的伤害增加 10%</li>
 * </ul>
 * Curios API 加载时项链佩戴于饰品栏；未加载时放置于副手生效。
 * <p>
 * 攻击力通过对 {@link Attributes#ATTACK_DAMAGE} 添加 {@link AttributeModifier}
 * （MULTIPLY_BASE）实现，防御效果通过 {@link LivingDamageEvent} 直接乘算。
 * 加成基于佩戴者上报的显存数据（见 {@link PerformanceMetrics}），未获取到数据时按 0 处理（无加成）。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonerNecklaceHandler {

    /** 攻击力修饰符固定 UUID，确保可被可靠移除 */
    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");

    private static final String ATTACK_DAMAGE_NAME = "commoner_necklace_damage";

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
        // 每 20 tick（1 秒）检查一次，覆盖饰品栏/副手装备变化
        if (player.tickCount % 20 != 0) {
            return;
        }
        updateAttackDamage(player);
    }

    /**
     * 受到伤害时按佩戴者显存结算特殊效果（垃圾佬减伤 / 无法理解平民增伤）。
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!hasNecklaceEquipped(victim)) {
            return;
        }
        int vramMB = PerformanceMetrics.getPlayerVramMB(victim);
        if (vramMB <= 0) {
            return;
        }
        double vramGB = vramMB / 1024.0;
        double defense = CommonerNecklaceItem.calculateDefenseMultiplier(vramGB);
        if (Math.abs(defense - 1.0) < 0.0001) {
            return;
        }
        event.setAmount((float) (event.getAmount() * defense));
    }

    /**
     * 判定项链是否生效：Curios 加载时检查饰品栏，未加载时检查副手。
     */
    private static boolean hasNecklaceEquipped(Player player) {
        if (isCuriosLoaded()) {
            return hasNecklaceInCurios(player);
        }
        return player.getOffhandItem().getItem() == EverlaartifactsModItems.COMMONER_NECKLACE.get();
    }

    /** 仅当 Curios 加载时调用，避免引用不存在的类 */
    private static boolean hasNecklaceInCurios(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(inventory -> inventory.isEquipped(EverlaartifactsModItems.COMMONER_NECKLACE.get()))
                .orElse(false);
    }

    private static void updateAttackDamage(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute == null) {
            return;
        }
        if (!hasNecklaceEquipped(player)) {
            attribute.removeModifier(ATTACK_DAMAGE_UUID);
            return;
        }
        int vramMB = PerformanceMetrics.getPlayerVramMB(player);
        if (vramMB <= 0) {
            attribute.removeModifier(ATTACK_DAMAGE_UUID);
            return;
        }
        double vramGB = vramMB / 1024.0;
        double bonus = CommonerNecklaceItem.calculateDamageMultiplier(vramGB) - 1.0;
        if (Math.abs(bonus) < 0.0001) {
            attribute.removeModifier(ATTACK_DAMAGE_UUID);
            return;
        }
        attribute.removeModifier(ATTACK_DAMAGE_UUID);
        attribute.addTransientModifier(new AttributeModifier(
                ATTACK_DAMAGE_UUID, ATTACK_DAMAGE_NAME, bonus,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
