package net.everla.everlaartifacts.server.handlers.enchantment;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Server-side handler for the "自由升级" (Escalation of Freedom) curse enchantment.
 * <p>
 * 所有效果在任意铠甲位佩戴该附魔时生效（造成的伤害减半同样作用于佩戴者发起攻击时）：
 * <ul>
 *   <li>造成的最终伤害降低 50% —— 修改 {@link LivingDamageEvent}（最终伤害）</li>
 *   <li>受到的最终伤害增加 100% —— 修改 {@link LivingDamageEvent}</li>
 *   <li>火焰类伤害提升 100% / 摔落伤害 +400% / 溺水伤害 +2000% —— 按伤害类型标签再乘一次</li>
 *   <li>受到的击退 100%~500% 随机 —— 修改 {@link LivingKnockBackEvent}</li>
 *   <li>最终护甲值降低 30% —— 对 {@link Attributes#ARMOR} 添加 -30% MULTIPLY_TOTAL 属性修饰符</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EscalationOfFreedomHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // ── 伤害倍率 ──────────────────────────────────────────────
    /** 造成的最终伤害降低50% */
    private static final double DEALT_DAMAGE_MULTIPLIER = 0.5;
    /** 受到的最终伤害增加100% */
    private static final double RECEIVED_DAMAGE_MULTIPLIER = 2.0;
    /** 火焰类伤害提升100%（受到的） */
    private static final double FIRE_DAMAGE_MULTIPLIER = 2.0;
    /** 摔落伤害增加400% */
    private static final double FALL_DAMAGE_MULTIPLIER = 5.0;
    /** 溺水伤害增加2000% */
    private static final double DROWNING_DAMAGE_MULTIPLIER = 21.0;

    // ── 最终护甲值降低30% 的属性修饰符 ─────────────────────────
    private static final UUID ARMOR_REDUCTION_UUID = UUID.fromString("4f2a1c3b-9e8d-4a6b-9c1e-7d5b8f3a2c4d");
    private static final double ARMOR_REDUCTION_PERCENT = 0.30;

    // ── 伤害处理 ──────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }

        DamageSource source = event.getSource();
        float amount = event.getAmount();

        // 造成者佩戴「自由升级」：造成的最终伤害降低50%
        if (source.getEntity() instanceof LivingEntity attacker && hasEscalationOfFreedom(attacker)) {
            amount *= DEALT_DAMAGE_MULTIPLIER;
        }

        // 承受者佩戴「自由升级」：受到的最终伤害增加100%
        if (hasEscalationOfFreedom(victim)) {
            amount *= RECEIVED_DAMAGE_MULTIPLIER;

            // 火焰类伤害提升100%
            if (source.is(DamageTypeTags.IS_FIRE)) {
                amount *= FIRE_DAMAGE_MULTIPLIER;
            }
            // 摔落伤害增加400%
            if (source.is(DamageTypeTags.IS_FALL)) {
                amount *= FALL_DAMAGE_MULTIPLIER;
            }
            // 溺水伤害增加2000%
            if (source.is(DamageTypeTags.IS_DROWNING)) {
                amount *= DROWNING_DAMAGE_MULTIPLIER;
            }
        }

        event.setAmount(amount);
    }

    // ── 击退处理 ──────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (hasEscalationOfFreedom(entity)) {
            // 受到的击退在100%~500%之间随机
            double multiplier = 1.0 + entity.level().random.nextDouble() * 4.0;
            event.setStrength(event.getStrength() * (float) multiplier);
        }
    }

    // ── 最终护甲值降低30% ─────────────────────────────────────

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        updateArmorReduction(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player.level().isClientSide()) {
            return;
        }
        // 周期性兜底（防换装事件遗漏），每1秒检查一次
        if (event.player.tickCount % 20 == 0) {
            updateArmorReduction(event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        updateArmorReduction(event.getEntity());
    }

    /**
     * 任意铠甲位佩戴该附魔即返回 {@code true}。
     */
    public static boolean hasEscalationOfFreedom(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
                    EverlaartifactsModEnchantments.ESCALATION_OF_FREEDOM.get(), stack) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 佩戴附魔时给 {@link Attributes#ARMOR} 挂上 -30% MULTIPLY_TOTAL 修饰符；
     * 脱下后移除。瞬态修饰符不会随存档持久化，因此登录/重生后由 tick 兜底重新挂上。
     */
    private static void updateArmorReduction(LivingEntity entity) {
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        if (hasEscalationOfFreedom(entity)) {
            if (armor.getModifier(ARMOR_REDUCTION_UUID) == null) {
                armor.addTransientModifier(new AttributeModifier(
                    ARMOR_REDUCTION_UUID,
                    "escalation_of_freedom_armor_reduction",
                    -ARMOR_REDUCTION_PERCENT,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        } else {
            armor.removeModifier(ARMOR_REDUCTION_UUID);
        }
    }
}
