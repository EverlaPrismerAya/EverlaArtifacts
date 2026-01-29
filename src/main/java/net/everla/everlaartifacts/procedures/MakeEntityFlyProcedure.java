package net.everla.everlaartifacts.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class MakeEntityFlyProcedure {
    // 全局音效冷却：记录上一次播放音效的服务器游戏 tick（所有维度共享）
    private static long lastSoundTick = -1L;

    public static void execute(LevelAccessor world, Entity entity) {
        if (entity == null || !entity.isAlive())
            return;

        // ===== 步骤1: 完全跳过物品实体（掉落物/经验球等）=====
        if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.tryParse("minecraft:items")))) {
            return; // 不击飞、不伤害、不播放音效
        }

        // ===== 步骤2: 飞行中的创造/旁观模式玩家完全免疫 =====
        // 注意：仅跳过"正在飞行"的创造模式玩家（地面创造玩家仍会被击飞但不受伤害）
        if (entity instanceof Player player) {
            // 旁观模式玩家始终免疫（无论是否"飞行"）
            if (player.isSpectator()) {
                return;
            }
            // 创造模式玩家仅在飞行状态时免疫
            if (player.isCreative() && player.getAbilities().flying) {
                return;
            }
        }

        // ===== 步骤3: 对所有非免疫实体应用击飞效果 =====
        entity.setDeltaMovement(new Vec3(
            (0.5 - Math.random()) * 6,
            (0.5 - Math.random()) * 6,
            (0.5 - Math.random()) * 6
        ));

        // ===== 步骤4: 服务端专属处理（音效+伤害）=====
        if (!world.isClientSide() && world instanceof ServerLevel level) {
            // ===== 音效频率限制：每服务器 tick 仅播放一次 =====
            long currentTick = level.getGameTime();
            boolean shouldPlaySound = (currentTick != lastSoundTick);
            
            if (shouldPlaySound) {
                // 播放音效（向所有玩家广播）
                level.getServer().getCommands().performPrefixedCommand(
                    new CommandSourceStack(
                        CommandSource.NULL,
                        entity.position(),
                        entity.getRotationVector(),
                        level,
                        4,
                        entity.getName().getString(),
                        entity.getDisplayName(),
                        level.getServer(),
                        entity
                    ),
                    "playsound everlaartifacts:auric_strike block @a ~ ~ ~ 0.2"
                );
                lastSoundTick = currentTick; // 更新最后播放 tick
            }

            // ===== 仅对生物实体造成伤害（非生物实体如盔甲架仅击飞）=====
            if (entity instanceof LivingEntity livingEntity) {
                // 安全检查：跳过无敌实体（包括地面创造模式玩家）
                if (entity.isInvulnerable()) {
                    return;
                }

                // 伤害计算：damage = (10 * log10(maxHealth + 1)) / 2 + 1
                double maxHealth = Math.max(1.0, livingEntity.getMaxHealth());
                float damage = (float) ((10.0 * Math.log10(maxHealth + 1)) / 2.0 + 1.0);

                // 造成爆炸伤害
                entity.hurt(level.damageSources().explosion(null, null), damage);
            }
            // 非LivingEntity（如盔甲架/矿车）→ 仅击飞，不造成伤害
        }
    }
}