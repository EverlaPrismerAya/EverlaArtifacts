
package net.everla.everlaartifacts.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class VenusShellPassiveMobEffect extends MobEffect {
	public VenusShellPassiveMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -103);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public List<ItemStack> getCurativeItems() {
		return List.of(); // 返回空列表，防止被牛奶等物品治愈
	}
}
