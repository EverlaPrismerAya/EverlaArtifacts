package net.everla.everlaartifacts;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.registries.ForgeRegistries;

public final class EverlaKillHandler {
    private EverlaKillHandler() {
        // 工具类禁止实例化
    }

    /**
     * 安全击杀玩家，自动处理无尽套免疫并触发标准死亡流程
     *
     * @param player       目标玩家（必须为服务端实体）
     * @param deathTag     用于PersistentData标记的死亡原因键（如 "everlaartifacts:suicide"）
     * @param deathMessage 自定义死亡消息组件
     * @param soundKey     音效资源位置（可为null，不播放音效）
     */
    public static void killPlayer(Player player, String deathTag, Component deathMessage, ResourceLocation soundKey) {
        if (player == null || player.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) player.level();

        // 步骤1: 检测并移除无尽四件套
        handleInfinityArmor(player, level);

        // 步骤2: 播放音效（如果提供）
        if (soundKey != null && ForgeRegistries.SOUND_EVENTS.containsKey(soundKey)) {
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ForgeRegistries.SOUND_EVENTS.getValue(soundKey),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
            );
        }

        // 步骤3: 生成爆炸粒子
        level.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            player.getX(),
            player.getY() + player.getBbHeight() * 0.5,
            player.getZ(),
            1,
            0.0,
            0.0,
            0.0,
            0.0
        );

        // 步骤4: 标记死亡原因
        player.getPersistentData().putBoolean(deathTag, true);

        // 步骤5: 广播死亡消息
        level.getServer().getPlayerList().broadcastSystemMessage(deathMessage, false);

        // 步骤6: 触发标准死亡流程（确保物品正常掉落）
        level.getServer().execute(() -> {
            if (!player.isRemoved() && player.isAlive()) {
                player.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            }
        });
    }

    // ========== 私有辅助方法 ==========

    private static void handleInfinityArmor(Player player, ServerLevel level) {
        boolean hasFullSet = isInfinityItem(player.getItemBySlot(EquipmentSlot.HEAD), "infinity_helmet") &&
                            isInfinityItem(player.getItemBySlot(EquipmentSlot.CHEST), "infinity_chestplate") &&
                            isInfinityItem(player.getItemBySlot(EquipmentSlot.LEGS), "infinity_pants") &&
                            isInfinityItem(player.getItemBySlot(EquipmentSlot.FEET), "infinity_boots");

        if (hasFullSet) {
            boolean keepInventory = level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
            storeOrDrop(player, level, player.getItemBySlot(EquipmentSlot.HEAD), keepInventory, EquipmentSlot.HEAD);
            storeOrDrop(player, level, player.getItemBySlot(EquipmentSlot.CHEST), keepInventory, EquipmentSlot.CHEST);
            storeOrDrop(player, level, player.getItemBySlot(EquipmentSlot.LEGS), keepInventory, EquipmentSlot.LEGS);
            storeOrDrop(player, level, player.getItemBySlot(EquipmentSlot.FEET), keepInventory, EquipmentSlot.FEET);
            player.containerMenu.broadcastFullState();
        }
    }

    private static boolean isInfinityItem(ItemStack stack, String expectedPath) {
        if (stack.isEmpty() || stack.is(Items.AIR)) return false;
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return registryName != null &&
               registryName.getNamespace().equals("avaritia") &&
               registryName.getPath().equals(expectedPath);
    }

    private static void storeOrDrop(Player player, ServerLevel level, ItemStack stack, boolean keepInventory, EquipmentSlot slot) {
        if (stack.isEmpty() || stack.is(Items.AIR)) return;
        ItemStack copy = stack.copy();
        if (keepInventory) {
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false, false);
            }
        } else {
            player.drop(copy, false, false);
        }
        player.setItemSlot(slot, ItemStack.EMPTY);
    }
}