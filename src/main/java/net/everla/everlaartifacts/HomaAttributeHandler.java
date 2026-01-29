package net.everla.everlaartifacts; // ← 请根据实际包名调整

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// MCreator 效果导入
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class HomaAttributeHandler {
    // 属性缓存（避免每 tick 重复更新）
    private static final Map<UUID, Double> PASSIVE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> ACTIVE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> CRIT_DAMAGE_CACHE = new ConcurrentHashMap<>();

    // 修饰符 UUID 生成器（基于玩家 UUID + 效果类型）
    private static UUID getPassiveUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("homa_passive_" + playerUUID).getBytes());
    }

    private static UUID getActiveUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("homa_active_" + playerUUID).getBytes());
    }

    private static UUID getCritDamageUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("homa_crit_damage_" + playerUUID).getBytes());
    }

    // 暴击伤害属性缓存（避免每 tick 重复查找）
    private static Attribute CRIT_DAMAGE_ATTRIBUTE = null;
    private static boolean ATTRIBUTESLIB_CHECKED = false;

    /**
     * 每 tick 检查玩家状态并更新属性
     * 仅在服务端运行（客户端自动同步）
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        updateHomaPassive(player);
        updateHomaActive(player);
        updateHomaCritDamage(player); // 新增：暴击伤害支持
    }

    /**
     * 更新护摩被动：基于最大生命值的 16% 攻击力
     * 生命 ≤50% 时额外 +18%（总计 34%）
     */
    private static void updateHomaPassive(Player player) {
        UUID uuid = player.getUUID();
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;

        UUID modifierUUID = getPassiveUUID(uuid);
        boolean hasPassive = player.hasEffect(EverlaartifactsModMobEffects.HOMA_PASSIVE.get());

        if (hasPassive) {
            double maxHealth = player.getMaxHealth();
            double currentHealth = player.getHealth();
            boolean isLowHealth = currentHealth <= maxHealth * 0.5;
            double newValue = maxHealth * (isLowHealth ? 0.34 : 0.16);

            if (!PASSIVE_CACHE.containsKey(uuid) || Math.abs(PASSIVE_CACHE.get(uuid) - newValue) > 0.01) {
                attackDamage.removeModifier(modifierUUID);
                AttributeModifier modifier = new AttributeModifier(
                    modifierUUID, "Homa Passive", newValue, AttributeModifier.Operation.ADDITION
                );
                attackDamage.addTransientModifier(modifier);
                PASSIVE_CACHE.put(uuid, newValue);
            }
        } else {
            attackDamage.removeModifier(modifierUUID);
            PASSIVE_CACHE.remove(uuid);
        }
    }

    /**
     * 更新护摩主动：基于最大生命值的 62.6% 攻击力
     */
    private static void updateHomaActive(Player player) {
        UUID uuid = player.getUUID();
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;

        UUID modifierUUID = getActiveUUID(uuid);
        boolean hasActive = player.hasEffect(EverlaartifactsModMobEffects.HOMA_ACTIVE.get());

        if (hasActive) {
            double newValue = player.getMaxHealth() * 0.626;

            if (!ACTIVE_CACHE.containsKey(uuid) || Math.abs(ACTIVE_CACHE.get(uuid) - newValue) > 0.01) {
                attackDamage.removeModifier(modifierUUID);
                AttributeModifier modifier = new AttributeModifier(
                    modifierUUID, "Homa Active", newValue, AttributeModifier.Operation.ADDITION
                );
                attackDamage.addTransientModifier(modifier);
                ACTIVE_CACHE.put(uuid, newValue);
            }
        } else {
            attackDamage.removeModifier(modifierUUID);
            ACTIVE_CACHE.remove(uuid);
        }
    }

    /**
     * 更新护摩暴击伤害：当 Apotheosis/AttributesLib 加载时，提供 66.2% 暴击伤害加成
     * 仅护摩被动生效时触发（符合原神设定）
     */
    private static void updateHomaCritDamage(Player player) {
        // 仅当 Apotheosis 或 AttributesLib 加载时生效
        if (!isAttributesLibAvailable()) return;

        UUID uuid = player.getUUID();
        AttributeInstance critDamage = player.getAttribute(CRIT_DAMAGE_ATTRIBUTE);
        if (critDamage == null) return;

        UUID modifierUUID = getCritDamageUUID(uuid);
        boolean hasPassive = player.hasEffect(EverlaartifactsModMobEffects.HOMA_PASSIVE.get());
        double newValue = 0.662; // 66.2% 暴击伤害加成

        if (hasPassive) {
            // 仅当值变化时更新（首次添加或值变化 >0.001）
            if (!CRIT_DAMAGE_CACHE.containsKey(uuid) || Math.abs(CRIT_DAMAGE_CACHE.get(uuid) - newValue) > 0.001) {
                critDamage.removeModifier(modifierUUID);
                AttributeModifier modifier = new AttributeModifier(
                    modifierUUID, "Homa Crit Damage", newValue, AttributeModifier.Operation.ADDITION
                );
                critDamage.addTransientModifier(modifier);
                CRIT_DAMAGE_CACHE.put(uuid, newValue);
            }
        } else {
            critDamage.removeModifier(modifierUUID);
            CRIT_DAMAGE_CACHE.remove(uuid);
        }
    }

    /**
     * 懒加载：检查 AttributesLib 是否可用并缓存暴击伤害属性
     * @return true if attributeslib:crit_damage 属性可用
     */
    private static boolean isAttributesLibAvailable() {
        if (!ATTRIBUTESLIB_CHECKED) {
            ATTRIBUTESLIB_CHECKED = true;
            // 检查模组是否加载
            if (ModList.get().isLoaded("apotheosis") || ModList.get().isLoaded("attributeslib")) {
                // 尝试获取暴击伤害属性
                CRIT_DAMAGE_ATTRIBUTE = ForgeRegistries.ATTRIBUTES.getValue(
                    new ResourceLocation("attributeslib", "crit_damage")
                );
            }
        }
        return CRIT_DAMAGE_ATTRIBUTE != null;
    }

    /**
     * 玩家死亡时清理缓存（防止内存泄漏）
     */
    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            UUID oldUUID = event.getOriginal().getUUID();
            PASSIVE_CACHE.remove(oldUUID);
            ACTIVE_CACHE.remove(oldUUID);
            CRIT_DAMAGE_CACHE.remove(oldUUID);
        }
    }
}