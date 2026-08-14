package net.everla.everlaartifacts.server.handlers.items.atm_ring;

import net.everla.everlaartifacts.common.item.AtmRingItem;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * ATM 之戒的效果处理（服务端）：
 * <ul>
 *   <li>每安装一个模组 → +0.03% 最终伤害</li>
 * </ul>
 * 模组数由客户端在进入游戏时上报（见 {@code ClientModCountPacket}）。
 * Curios API 加载时戒指佩戴于饰品栏；未加载时放置于副手生效。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AtmRingHandler {

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

        int modCount = PerformanceMetrics.getPlayerModCount(attacker.getUUID());
        double damageMultiplier = AtmRingItem.calculateDamageMultiplier(modCount);

        event.setAmount((float) (event.getAmount() * damageMultiplier));
    }

    /**
     * 判定戒指是否生效：Curios 加载时检查饰品栏，未加载时检查副手。
     */
    private static boolean hasRingEquipped(Player player) {
        if (isCuriosLoaded()) {
            return hasRingInCurios(player);
        }
        return player.getOffhandItem().getItem() == EverlaartifactsModItems.ATM_RING.get();
    }

    /** 仅当 Curios 加载时调用，避免引用不存在的类 */
    private static boolean hasRingInCurios(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(inventory -> inventory.isEquipped(EverlaartifactsModItems.ATM_RING.get()))
                .orElse(false);
    }
}
