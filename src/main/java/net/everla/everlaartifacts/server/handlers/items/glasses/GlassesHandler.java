package net.everla.everlaartifacts.server.handlers.items.glasses;

import net.everla.everlaartifacts.common.item.GlassesItem;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
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
 * 近视眼镜的效果处理（服务端）：
 * <ul>
 *   <li>基于使用者当前分辨率决定攻击力：1920×1080 为 0%，800×600 最高 +35%，3840×2160 最低 -60%</li>
 * </ul>
 * 分辨率由客户端通过实时性能包周期性上报（见 {@code ClientPerformanceStatusPacket}）。
 * 攻击力通过对 {@link Attributes#ATTACK_DAMAGE} 添加 {@link AttributeModifier}
 * （MULTIPLY_BASE）实现。Curios API 加载时作为饰品佩戴；未加载时放在头盔槽位生效。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlassesHandler {

    /** 攻击力修饰符固定 UUID，确保可被可靠移除 */
    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("9f8e7d6c-5b4a-4c3d-2e1f-0a9b8c7d6e5f");

    private static final String ATTACK_DAMAGE_NAME = "glasses_attack_damage";

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
     * 判定眼镜是否生效：原版头盔槽，或 Curios 加载时的饰品栏。
     */
    private static boolean hasGlassesEquipped(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == EverlaartifactsModItems.GLASSES.get()) {
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
                .map(inventory -> inventory.isEquipped(EverlaartifactsModItems.GLASSES.get()))
                .orElse(false);
    }

    private static void updateAttackDamage(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute == null) {
            return;
        }
        if (!hasGlassesEquipped(player)) {
            attribute.removeModifier(ATTACK_DAMAGE_UUID);
            return;
        }
        int width = PerformanceMetrics.getPlayerWindowWidth(player.getUUID());
        int height = PerformanceMetrics.getPlayerWindowHeight(player.getUUID());
        double bonus = GlassesItem.calculateDamageMultiplier(width, height) - 1.0;
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
