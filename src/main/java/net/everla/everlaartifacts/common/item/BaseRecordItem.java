package net.everla.everlaartifacts.common.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.function.Supplier;

public class BaseRecordItem extends RecordItem {
    private final String descriptionKey;

    public BaseRecordItem(int comparatorValue, String soundEventName, int lengthInTicks) {
        this(comparatorValue, soundEventName, lengthInTicks, null);
    }

    public BaseRecordItem(int comparatorValue, String soundEventName, int lengthInTicks, String descriptionKey) {
        super(comparatorValue, 
              () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:" + soundEventName)), 
              new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 
              lengthInTicks);
        this.descriptionKey = descriptionKey;
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, level, list, flag);
        if (descriptionKey != null) {
            list.add(Component.translatable(descriptionKey));
        }
    }
}