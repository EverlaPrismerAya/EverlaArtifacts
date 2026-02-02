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

                            list.add(Component.translatable("item.everlaartifacts.homa_staff.description_17_5"));
                            list.add(Component.translatable("item.everlaartifacts.homa_staff.description_17_75"));
                            
                            list.add(Component.translatable("item.everlaartifacts.homa_staff.description_f0"));
                            list.add(Component.translatable("item.everlaartifacts.homa_staff.description_f1"));
                            list.add(Component.translatable("item.everlaartifacts.homa_staff.description_f2"));
                            
                            list.add(Component.translatable("item.everlaartifacts.homa_staff.description_19"));
                            list.add(Component.translatable("item.everlaartifacts.homa_staff.description_20"));
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

    @Override
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (selected && entity instanceof Player player) {
            HomaTickFuncProcedure.execute(player);
        }
    }
}