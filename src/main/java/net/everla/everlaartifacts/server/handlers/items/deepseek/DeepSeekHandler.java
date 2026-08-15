package net.everla.everlaartifacts.server.handlers.items.deepseek;

import net.everla.everlaartifacts.common.item.DeepSeekItem;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/**
 * 深度求索之戒的效果处理（服务端）：
 * <ul>
 *   <li>基于使用者当前 CPU 利用率提升攻击力：40% 为 0%，50% 最高 +25%，20% 最低 -25%</li>
 * </ul>
 * CPU 利用率由客户端周期性上报（见 {@code ClientPerformanceStatusPacket}）。
 * 攻击力通过对 {@link Attributes#ATTACK_DAMAGE} 添加 {@link AttributeModifier}
 * （MULTIPLY_BASE）实现。Curios API 加载时戒指佩戴于饰品栏；未加载时放置于副手生效。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeepSeekHandler {

    /** 攻击力修饰符固定 UUID，确保可被可靠移除 */
    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("6a1b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");

    private static final String ATTACK_DAMAGE_NAME = "deepseek_attack_damage";

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
     * 判定戒指是否生效：Curios 加载时检查饰品栏，未加载时检查副手。
     */
    private static boolean hasRingEquipped(Player player) {
        if (isCuriosLoaded()) {
            return hasRingInCurios(player);
        }
        return player.getOffhandItem().getItem() == EverlaartifactsModItems.DEEPSEEK.get();
    }

    /** 仅当 Curios 加载时调用，避免引用不存在的类 */
    private static boolean hasRingInCurios(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(inventory -> inventory.isEquipped(EverlaartifactsModItems.DEEPSEEK.get()))
                .orElse(false);
    }

    private static void updateAttackDamage(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute == null) {
            return;
        }
        if (!hasRingEquipped(player)) {
            attribute.removeModifier(ATTACK_DAMAGE_UUID);
            return;
        }
        int cpuLoad = PerformanceMetrics.getPlayerCpuLoad(player.getUUID());
        double bonus = DeepSeekItem.calculateDamageMultiplier(cpuLoad) - 1.0;
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
