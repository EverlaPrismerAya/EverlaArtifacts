package net.everla.everlaartifacts.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.everla.everlaartifacts.server.PerformanceMetrics;
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
 * 近视眼镜：基于使用者的分辨率决定攻击力。
 * <p>
 * 1920×1080 为基准线 0%；分辨率越低攻击力越高，最低 800×600 时 +35%；
 * 分辨率越高攻击力越低，最高 3840×2160 时 -60%（按像素数分段线性，超范围钳制）。
 * <p>
 * 既是 Curios 饰品，也是原版头盔——Curios API 未加载时放在头盔槽位同样生效。
 * 无限耐久、0 护甲值。攻击力通过对 generic.attack_damage 添加 MULTIPLY_BASE
 * 属性修饰符实现（见 {@code GlassesHandler}）。
 */
public class GlassesItem extends ArmorItem {

	// 基准线 1920×1080 = 0% 加成
	public static final int BASE_WIDTH = 1920;
	public static final int BASE_HEIGHT = 1080;
	public static final long BASE_PIXELS = 2_073_600L;
	// 最低分辨率 800×600 → +35%
	public static final int MIN_WIDTH = 800;
	public static final int MIN_HEIGHT = 600;
	public static final long MIN_PIXELS = 480_000L;
	// 最高分辨率 3840×2160 → -60%
	public static final int MAX_WIDTH = 3840;
	public static final int MAX_HEIGHT = 2160;
	public static final long MAX_PIXELS = 8_294_400L;
	public static final double MAX_BONUS = 0.35;
	public static final double MIN_BONUS = -0.60;

	public GlassesItem() {
		super(new GlassesArmorMaterial(), ArmorItem.Type.HELMET,
				new Item.Properties().rarity(Rarity.EPIC));
	}

	/**
	 * 由窗口分辨率（宽×高，像素）计算攻击力倍率。
	 * <p>
	 * 按像素总数分段线性：800×600→+35%，1920×1080→0%，3840×2160→-60%，
	 * 低于 800×600 钳制为 +35%，高于 3840×2160 钳制为 -60%。
	 *
	 * @param width  窗口宽度（像素）
	 * @param height 窗口高度（像素）
	 * @return 攻击力倍率（1.0 表示无加成）
	 */
	public static double calculateDamageMultiplier(int width, int height) {
		int w = width <= 0 ? BASE_WIDTH : width;
		int h = height <= 0 ? BASE_HEIGHT : height;
		long pixels = (long) w * h;

		double bonus;
		if (pixels <= MIN_PIXELS) {
			bonus = MAX_BONUS;
		} else if (pixels < BASE_PIXELS) {
			double ratio = (double) (pixels - MIN_PIXELS) / (BASE_PIXELS - MIN_PIXELS);
			bonus = MAX_BONUS * (1.0 - ratio);
		} else if (pixels < MAX_PIXELS) {
			double ratio = (double) (pixels - BASE_PIXELS) / (MAX_PIXELS - BASE_PIXELS);
			bonus = MIN_BONUS * ratio;
		} else {
			bonus = MIN_BONUS;
		}
		return 1.0 + bonus;
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		// 无限耐久
		return false;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot, ItemStack stack) {
		// 返回空属性修饰符，隐藏原版护甲自带的 "戴在头上时：+0 护甲值" 等提示行
		return ImmutableMultimap.of();
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		// description_0 显示当前分辨率下的实际攻击力提升
		int width = PerformanceMetrics.getLatestClientWindowWidth();
		int height = PerformanceMetrics.getLatestClientWindowHeight();
		double damageBonus = calculateDamageMultiplier(width, height) - 1.0;
		tooltip.add(Component.translatable("item.everlaartifacts.glasses.description_0",
				String.format("%+.2f%%", damageBonus * 100.0), width, height));
		tooltip.add(Component.translatable("item.everlaartifacts.glasses.description_1"));
	}

	/** 0 护甲值、无限耐久的头盔材质 */
	private static class GlassesArmorMaterial implements ArmorMaterial {
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
			return "everlaartifacts:glasses";
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
