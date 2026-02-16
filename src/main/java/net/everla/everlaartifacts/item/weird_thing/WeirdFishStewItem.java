package net.everla.everlaartifacts.item.weird_thing;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

public class WeirdFishStewItem extends Item {
	public WeirdFishStewItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).food((new FoodProperties.Builder()).nutrition(4).saturationMod(6.0f).alwaysEat().build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = new ItemStack(Items.BOWL);
		super.finishUsingItem(itemstack, world, entity);
		
		// 给玩家添加3分钟的致命毒素效果 (180秒 = 3600 ticks)
		entity.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.LETHAL_POISON.get(), 3600, 0, false, true));
		
		// 给玩家添加3分钟的认知错乱效果 (180秒 = 3600 ticks)
		entity.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.COGNITIVE_DISORDER.get(), 3600, 0, false, true));
		
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
}