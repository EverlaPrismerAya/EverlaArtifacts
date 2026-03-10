package net.everla.everlaartifacts.common.item.weird_thing;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WeirdCocktailItem extends Item {
	public WeirdCocktailItem() {
		super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.3f).alwaysEat().build()));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.DRINK;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, level, list, flag);
		list.add(Component.translatable("item.everlaartifacts.weird_cocktail.description_0"));
		list.add(Component.translatable("item.everlaartifacts.weird_cocktail.description_1"));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = new ItemStack(Items.GLASS_BOTTLE);
		super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		applyWeirdCocktailEffects(entity);
		if (itemstack.isEmpty()) {
			return retval;
		} else {
			if (entity instanceof Player player && !player.getAbilities().instabuild) {
				if (!player.getInventory().add(retval))
					player.drop(retval, false);
			}
			return itemstack;
		}
	}

	private static void applyWeirdCocktailEffects(Entity entity) {
		if (entity == null || !(entity instanceof LivingEntity livingEntity) || entity.level().isClientSide()) {
			return;
		}

		// 应用所有药水效果
		livingEntity.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.BEDMIC_DESTRUCTION.get(), 6000, 0, false, true));
		livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 9, false, true));
		livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 1, false, true));
		livingEntity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2, false, true));
		livingEntity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 14, false, true));
	}
}