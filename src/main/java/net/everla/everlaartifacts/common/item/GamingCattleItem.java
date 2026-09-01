package net.everla.everlaartifacts.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电竞牛头：基于佩戴者当前 FPS 获得状态效果（低 FPS 增益，高 FPS 减益）。
 * <p>
 * 既是 Curios 饰品（首饰），也是原版头盔——Curios API 未加载时放在头盔槽位
 * 同样生效。无限耐久、0 护甲值。
 */
public class GamingCattleItem extends ArmorItem {
	public GamingCattleItem() {
		super(new GamingCattleArmorMaterial(), ArmorItem.Type.HELMET,
				new Item.Properties().rarity(Rarity.EPIC));
	}

	/** 效果位定义：从低位到高位依次对应低 FPS 增益与高 FPS 减益 */
	private static final int BIT_STRENGTH = 1;          // 力量 I
	private static final int BIT_HASTE = 1 << 1;        // 急迫 II
	private static final int BIT_SPEED = 1 << 2;        // 速度 II
	private static final int BIT_REGENERATION = 1 << 3; // 生命恢复 II
	private static final int BIT_RESISTANCE = 1 << 4;   // 抗性提升 II
	private static final int BIT_WEAKNESS = 1 << 5;     // 虚弱 I
	private static final int BIT_SLOW = 1 << 6;         // 缓慢 II
	private static final int BIT_FATIGUE = 1 << 7;      // 挖掘疲劳 II
	private static final int BIT_HUNGER = 1 << 8;       // 饥饿 II
	private static final int BIT_POISON = 1 << 9;       // 剧毒 II

	/**
	 * 由当前 FPS 计算应施加的状态效果掩码。
	 * <p>
	 * 低 FPS 增益：&lt;80 力量I、&lt;60 急迫II+速度II、&lt;40 生命恢复II、&lt;20 抗性提升II；
	 * 高 FPS 减益：&gt;100 虚弱I、&gt;120 缓慢II+挖掘疲劳II、&gt;140 饥饿II、&gt;160 剧毒II。
	 * <p>
	 * 同时供客户端「结果变动才上报」判断与 {@code GamingCattleHandler} 施效使用，
	 * 保证两端阈值一致。
	 *
	 * @param fps 当前 FPS
	 * @return 效果位掩码（0 表示无效果）
	 */
	public static int targetEffectMask(double fps) {
		int mask = 0;
		if (fps < 80.0) {
			mask |= BIT_STRENGTH;
		}
		if (fps < 60.0) {
			mask |= BIT_HASTE;
			mask |= BIT_SPEED;
		}
		if (fps < 40.0) {
			mask |= BIT_REGENERATION;
		}
		if (fps < 20.0) {
			mask |= BIT_RESISTANCE;
		}
		if (fps > 100.0) {
			mask |= BIT_WEAKNESS;
		}
		if (fps > 120.0) {
			mask |= BIT_SLOW;
			mask |= BIT_FATIGUE;
		}
		if (fps > 140.0) {
			mask |= BIT_HUNGER;
		}
		if (fps > 160.0) {
			mask |= BIT_POISON;
		}
		return mask;
	}

	/**
	 * 由效果位掩码还原目标效果集（效果 → 等级放大器）。
	 *
	 * @param mask 效果位掩码
	 * @return 效果 → 等级放大器 映射（可为空）
	 */
	public static Map<MobEffect, Integer> effectsFromMask(int mask) {
		Map<MobEffect, Integer> target = new HashMap<>();
		if ((mask & BIT_STRENGTH) != 0) {
			target.put(MobEffects.DAMAGE_BOOST, 0);
		}
		if ((mask & BIT_HASTE) != 0) {
			target.put(MobEffects.DIG_SPEED, 1);
		}
		if ((mask & BIT_SPEED) != 0) {
			target.put(MobEffects.MOVEMENT_SPEED, 1);
		}
		if ((mask & BIT_REGENERATION) != 0) {
			target.put(MobEffects.REGENERATION, 1);
		}
		if ((mask & BIT_RESISTANCE) != 0) {
			target.put(MobEffects.DAMAGE_RESISTANCE, 1);
		}
		if ((mask & BIT_WEAKNESS) != 0) {
			target.put(MobEffects.WEAKNESS, 0);
		}
		if ((mask & BIT_SLOW) != 0) {
			target.put(MobEffects.MOVEMENT_SLOWDOWN, 1);
		}
		if ((mask & BIT_FATIGUE) != 0) {
			target.put(MobEffects.DIG_SLOWDOWN, 1);
		}
		if ((mask & BIT_HUNGER) != 0) {
			target.put(MobEffects.HUNGER, 1);
		}
		if ((mask & BIT_POISON) != 0) {
			target.put(MobEffects.POISON, 1);
		}
		return target;
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		// 无限耐久
		return false;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot, ItemStack stack) {
		// 返回空属性修饰符，隐藏原版护甲自带的 "戴在头上时：+0 护甲值" 等提示行，
		// 只保留自定义简介文本（0 护甲本身无属性加成，不影响实际数值）
		return ImmutableMultimap.of();
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.translatable("item.everlaartifacts.gaming_cattle.description_0"));
		tooltip.add(Component.translatable("item.everlaartifacts.gaming_cattle.description_1"));
	}

	/** 0 护甲值、无限耐久的头盔材质 */
	private static class GamingCattleArmorMaterial implements ArmorMaterial {
		@Override
		public int getDurabilityForType(ArmorItem.Type type) {
			// 数值无实际意义，无限耐久由 isDamageable()=false 保证
			return 999999;
		}

		@Override
		public int getDefenseForType(ArmorItem.Type type) {
			return 0; // 0 护甲值
		}

		@Override
		public int getEnchantmentValue() {
			return 0;
		}

		@Override
		public SoundEvent getEquipSound() {
			return SoundEvents.ARMOR_EQUIP_LEATHER;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.EMPTY;
		}

		@Override
		public String getName() {
			return "everlaartifacts:gaming_cattle";
		}

		@Override
		public float getToughness() {
			return 0.0F;
		}

		@Override
		public float getKnockbackResistance() {
			return 0.0F;
		}
	}
}
