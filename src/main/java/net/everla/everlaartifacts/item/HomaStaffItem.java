package net.everla.everlaartifacts.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;

import net.everla.everlaartifacts.procedures.HomaTickFuncProcedure;
import net.everla.everlaartifacts.procedures.HomaStaffFuncProcedure;

import java.util.List;

public class HomaStaffItem extends SwordItem {
    public HomaStaffItem() {
        super(new Tier() {
            public int getUses() {
                return 715;
            }

            public float getSpeed() {
                return 4f;
            }

            public float getAttackDamageBonus() {
                return 3.7f;
            }

            public int getLevel() {
                return 4;
            }

            public int getEnchantmentValue() {
                return 2;
            }

            public Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.NETHERITE_INGOT));
            }
        }, 3, -2f, new Item.Properties().fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        // 右击：保持原功能（非潜行专属）
        InteractionResultHolder<ItemStack> ar = super.use(world, player, hand);
        HomaStaffFuncProcedure.execute(player, ar.getObject());
        return ar;
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, level, list, flag);
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_n0"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_n1"));
        
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_0"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_1"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_2"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_3"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_4"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_5"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_6"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_7"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_8"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_9"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_10"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_11"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_12"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_13"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_14"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_15"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_16"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_17"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_18"));
        
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_f0"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_f1"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_f2"));
        
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_19"));
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_20"));
        
        list.add(Component.translatable("item.everlaartifacts.homa_staff.description_21"));
    }

    @Override
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (selected && entity instanceof Player player) {
            HomaTickFuncProcedure.execute(player);
        }
    }
}