package net.everla.everlaartifacts.common.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public class AuricScrapBlockItem extends BlockItem {
    public AuricScrapBlockItem(Block block) {
        super(block, new Properties().fireResistant().rarity(Rarity.EPIC));
    }
}