package net.everla.everlaartifacts.server.handlers.items.venus_shell;

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
public class VenusShellAttributeHandler {
    // 属性缓存（避免每 tick 重复更新）
    private static final Map<UUID, Double> CRIT_CHANCE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> CRIT_DAMAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> ARMOR_CRIT_DAMAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> ATTACK_DAMAGE_CACHE = new ConcurrentHashMap<>();

    // 修饰符 UUID 生成器（基于玩家 UUID + 属性类型）
    private static UUID getCritChanceUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("venus_shell_crit_chance_" + playerUUID).getBytes());
    }

    private static UUID getCritDamageUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("venus_shell_crit_damage_" + playerUUID).getBytes());
    }

    private static UUID getArmorCritDamageUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("venus_shell_armor_crit_damage_" + playerUUID).getBytes());
    }

    private static UUID getAttackDamageUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("venus_shell_attack_damage_" + playerUUID).getBytes());
    }

    // 属性缓存（避免每 tick 重复查找）
    private static Attribute CRIT_CHANCE_ATTRIBUTE = null;
    private static Attribute CRIT_DAMAGE_ATTRIBUTE = null;
    private static boolean ATTRIBUTESLIB_CHECKED = false;
    
    // 用于控制属性更新频率的计数器
    private static final java.util.Map<java.util.UUID, Integer> playerUpdateCounter = new ConcurrentHashMap<>();

    /**
     * 每 tick 检查玩家状态并更新属性
     * 仅在服务端运行（客户端自动同步）
     * 优化：每5个tick更新一次，减少性能开销
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // 每5个tick更新一次，减少性能开销
        UUID playerUUID = player.getUUID();
        int currentTick = playerUpdateCounter.getOrDefault(playerUUID, 0) + 1;
        playerUpdateCounter.put(playerUUID, currentTick);
        
        if (currentTick % 5 != 0) { // 每5个tick更新一次
            return;
        }

        updateVenusShellAttributes(player);
    }

    /**
     * 更新Venus Shell所有属性加成
     * 仅当加载了Apotheosis或AttributesLib模组时生效
     */
    private static void updateVenusShellAttributes(Player player) {
        // 检查是否具有Venus Shell Passive效果
        boolean hasVenusShellPassive = player.hasEffect(EverlaartifactsModMobEffects.VENUS_SHELL_PASSIVE.get());
        
        if (!hasVenusShellPassive) {
            // 如果没有效果，清除所有修饰符
            clearAllModifiers(player);
            return;
        }

        // 仅当 AttributesLib 可用时处理属性
        if (!isAttributesLibAvailable()) {
            return;
        }

        // 更新暴击率：22.1%
        updateCritChance(player);
        
        // 更新基础暴击伤害：20%
        updateBaseCritDamage(player);
        
        // 更新基于盔甲韧性的暴击伤害：220%盔甲韧性
        updateArmorBasedCritDamage(player);
        
        // 更新基于护甲值的攻击力：16%护甲值
        updateArmorBasedAttackDamage(player);
    }

    /**
     * 更新暴击率属性（22.1%）
     */
    private static void updateCritChance(Player player) {
        AttributeInstance critChance = player.getAttribute(CRIT_CHANCE_ATTRIBUTE);
        if (critChance == null) return;

        UUID uuid = player.getUUID();
        UUID modifierUUID = getCritChanceUUID(uuid);
        double newValue = 0.221; // 22.1% 暴击率

        if (!CRIT_CHANCE_CACHE.containsKey(uuid) || Math.abs(CRIT_CHANCE_CACHE.get(uuid) - newValue) > 0.001) {
            critChance.removeModifier(modifierUUID);
            AttributeModifier modifier = new AttributeModifier(
                modifierUUID, "Venus Shell Crit Chance", newValue, AttributeModifier.Operation.ADDITION
            );
            critChance.addTransientModifier(modifier);
            CRIT_CHANCE_CACHE.put(uuid, newValue);
        }
    }

    /**
     * 更新基础暴击伤害属性（20%）
     */
    private static void updateBaseCritDamage(Player player) {
        AttributeInstance critDamage = player.getAttribute(CRIT_DAMAGE_ATTRIBUTE);
        if (critDamage == null) return;

        UUID uuid = player.getUUID();
        UUID modifierUUID = getCritDamageUUID(uuid);
        double newValue = 0.20; // 20% 暴击伤害

        if (!CRIT_DAMAGE_CACHE.containsKey(uuid) || Math.abs(CRIT_DAMAGE_CACHE.get(uuid) - newValue) > 0.001) {
            critDamage.removeModifier(modifierUUID);
            AttributeModifier modifier = new AttributeModifier(
                modifierUUID, "Venus Shell Base Crit Damage", newValue, AttributeModifier.Operation.ADDITION
            );
            critDamage.addTransientModifier(modifier);
            CRIT_DAMAGE_CACHE.put(uuid, newValue);
        }
    }

    /**
     * 更新基于盔甲韧性的暴击伤害（220%盔甲韧性）
     */
    private static void updateArmorBasedCritDamage(Player player) {
        AttributeInstance critDamage = player.getAttribute(CRIT_DAMAGE_ATTRIBUTE);
        if (critDamage == null) return;

        UUID uuid = player.getUUID();
        UUID modifierUUID = getArmorCritDamageUUID(uuid);
        
        // 获取玩家当前盔甲韧性
        AttributeInstance armorToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        double currentArmorToughness = armorToughness != null ? armorToughness.getValue() : 0.0;
        double newValue = currentArmorToughness * 0.022; // 220%盔甲韧性

        if (!ARMOR_CRIT_DAMAGE_CACHE.containsKey(uuid) || Math.abs(ARMOR_CRIT_DAMAGE_CACHE.get(uuid) - newValue) > 0.01) {
            critDamage.removeModifier(modifierUUID);
            AttributeModifier modifier = new AttributeModifier(
                modifierUUID, "Venus Shell Armor Crit Damage", newValue, AttributeModifier.Operation.ADDITION
            );
            critDamage.addTransientModifier(modifier);
            ARMOR_CRIT_DAMAGE_CACHE.put(uuid, newValue);
        }
    }

    /**
     * 更新基于护甲值的攻击力（16%护甲值）
     */
    private static void updateArmorBasedAttackDamage(Player player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;

        UUID uuid = player.getUUID();
        UUID modifierUUID = getAttackDamageUUID(uuid);
        
        // 获取玩家当前护甲值
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        double currentArmor = armor != null ? armor.getValue() : 0.0;
        double newValue = currentArmor * 0.16; // 16%护甲值

        if (!ATTACK_DAMAGE_CACHE.containsKey(uuid) || Math.abs(ATTACK_DAMAGE_CACHE.get(uuid) - newValue) > 0.01) {
            attackDamage.removeModifier(modifierUUID);
            AttributeModifier modifier = new AttributeModifier(
                modifierUUID, "Venus Shell Armor Attack Damage", newValue, AttributeModifier.Operation.ADDITION
            );
            attackDamage.addTransientModifier(modifier);
            ATTACK_DAMAGE_CACHE.put(uuid, newValue);
        }
    }

    /**
     * 清除所有Venus Shell相关的属性修饰符
     */
    private static void clearAllModifiers(Player player) {
        UUID uuid = player.getUUID();
        
        // 清除暴击率修饰符
        AttributeInstance critChance = player.getAttribute(CRIT_CHANCE_ATTRIBUTE);
        if (critChance != null) {
            critChance.removeModifier(getCritChanceUUID(uuid));
        }
        CRIT_CHANCE_CACHE.remove(uuid);

        // 清除基础暴击伤害修饰符
        AttributeInstance critDamage = player.getAttribute(CRIT_DAMAGE_ATTRIBUTE);
        if (critDamage != null) {
            critDamage.removeModifier(getCritDamageUUID(uuid));
            critDamage.removeModifier(getArmorCritDamageUUID(uuid));
        }
        CRIT_DAMAGE_CACHE.remove(uuid);
        ARMOR_CRIT_DAMAGE_CACHE.remove(uuid);

        // 清除攻击力修饰符
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(getAttackDamageUUID(uuid));
        }
        ATTACK_DAMAGE_CACHE.remove(uuid);
    }

    /**
     * 懒加载：检查 AttributesLib 是否可用并缓存属性
     * @return true if attributeslib attributes are available
     */
    private static boolean isAttributesLibAvailable() {
        if (!ATTRIBUTESLIB_CHECKED) {
            ATTRIBUTESLIB_CHECKED = true;
            // 检查模组是否加载
            if (ModList.get().isLoaded("apotheosis") || ModList.get().isLoaded("attributeslib")) {
                // 获取暴击率属性
                CRIT_CHANCE_ATTRIBUTE = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("attributeslib", "crit_chance"));
                // 获取暴击伤害属性
                CRIT_DAMAGE_ATTRIBUTE = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("attributeslib", "crit_damage"));
            }
        }
        return CRIT_CHANCE_ATTRIBUTE != null && CRIT_DAMAGE_ATTRIBUTE != null;
    }

    /**
     * 玩家死亡时清理缓存（防止内存泄漏）
     */
    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            UUID oldUUID = event.getOriginal().getUUID();
            CRIT_CHANCE_CACHE.remove(oldUUID);
            CRIT_DAMAGE_CACHE.remove(oldUUID);
            ARMOR_CRIT_DAMAGE_CACHE.remove(oldUUID);
            ATTACK_DAMAGE_CACHE.remove(oldUUID);
        }
    }
}