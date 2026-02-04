
package net.everla.everlaartifacts.item;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;

import java.util.List;

public class VenusShellItem extends SwordItem {
	public VenusShellItem() {
		super(new Tier() {
			public int getUses() {
				return 2025;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 4.2f;
			}

			public int getLevel() {
				return 4;
			}

			public int getEnchantmentValue() {
				return 15;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(Blocks.PRISMARINE), new ItemStack(Items.NAUTILUS_SHELL));
			}
		}, 3, -2f, new Item.Properties().fireResistant());
	}

	@Override
	public boolean hasCraftingRemainingItem(ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
		return new ItemStack(this);
	}

	@Override
	public boolean isRepairable(ItemStack itemstack) {
		return false;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, level, list, flag);
		
		// 检查是否在客户端主线程上运行
		if (level != null && level.isClientSide()) {
			try {
				// 检查FML环境是否为客户端，这在构建搜索索引时通常是安全的
				if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
					// 在构建搜索索引时，有时screen可能为null，但Minecraft实例存在
					// 我们需要检测是否在GUI渲染上下文之外（例如搜索索引构建）
					net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
					
					// 如果mc为null或mc.screen为null，这可能意味着我们不在GUI上下文中
					if (mc != null && mc.screen != null) {
						// 在正常的GUI上下文中，可以安全地检查Shift键
						boolean isShiftKeyDown = net.minecraft.client.gui.screens.Screen.hasShiftDown();
						
						if (isShiftKeyDown) {
							// 显示详细信息（按住Shift时显示）
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_0"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_1"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_2"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_3"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_4"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_5"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_6"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_7"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_8"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_9"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_10"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_11"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_12"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_13"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_14"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_15"));
							list.add(Component.translatable("item.everlaartifacts.venus_shell.description_16"));
						} else {
							// 没有按Shift键时显示提示信息
							list.add(Component.translatable("item.everlaartifacts.universal_shift_notify"));
						}
					} else {
						// 不在GUI上下文（如构建搜索索引时），只显示基本提示
						list.add(Component.translatable("item.everlaartifacts.universal_shift_notify"));
					}
				} else {
					// 不在客户端环境
					list.add(Component.translatable("item.everlaartifacts.universal_shift_notify"));
				}
			} catch (Exception e) {
				// 如果发生任何异常，只显示基本提示
				list.add(Component.translatable("item.everlaartifacts.universal_shift_notify"));
			}
		} else {
			// 服务端环境
			list.add(Component.translatable("item.everlaartifacts.universal_shift_notify"));
		}
	}
}
