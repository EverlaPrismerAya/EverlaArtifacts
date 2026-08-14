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
import net.minecraft.world.level.Level;

import java.util.List;

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
