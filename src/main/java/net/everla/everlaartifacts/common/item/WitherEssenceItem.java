package net.everla.everlaartifacts.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class WitherEssenceItem extends Item {
    public WitherEssenceItem() {
        super(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, level, list, flag);
        list.add(Component.translatable("item.everlaartifacts.misc.lunatic"));
    }
}
