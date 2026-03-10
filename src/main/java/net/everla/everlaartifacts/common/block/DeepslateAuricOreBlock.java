package net.everla.everlaartifacts.common.block;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DeepslateAuricOreBlock extends Block {
    // 静态冷却计数器：防止同一 tick 内重复生成粒子或播放音效
    private static int lastParticleTick = -1;
    private static long lastSoundTick = -1L;

    public DeepslateAuricOreBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.ANCIENT_DEBRIS)
            .strength(45f, 1200f)
            .lightLevel(s -> 2)
            .requiresCorrectToolForDrops()
            .speedFactor(1.4f)
            .jumpFactor(1.5f)
            .noOcclusion()
            .isRedstoneConductor((bs, br, bp) -> false));
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 15;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return box(1, 1, 1, 15, 15, 15); // 14x14x14 内部立方体
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState blockstate, BlockGetter blockAccess, BlockPos pos, net.minecraft.core.Direction direction) {
        return 2;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.core.Direction side) {
        return true;
    }

    @Override
    public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
        super.entityInside(blockstate, world, pos, entity);
        auricOreDamage(world, entity);

        // ===== 灵魂火焰粒子：仅在服务端生成，且每 tick 限1次 =====
        if (!world.isClientSide() && world instanceof ServerLevel serverLevel) {
            int currentTick = (int) serverLevel.getGameTime();
            
            // 全局冷却：同一 tick 内只生成1次粒子
            if (currentTick == lastParticleTick) {
                return;
            }
            lastParticleTick = currentTick;

            // 方块实际占据范围：1/16 ~ 15/16（14格内部空间）
            final double MIN = 1.0 / 16.0;
            final double MAX = 15.0 / 16.0;

            // 定义12条棱的端点（局部坐标，每条棱由两个端点定义）
            double[][][] edges = {
                // 底部4条 (y=MIN)
                {{MIN, MIN, MIN}, {MAX, MIN, MIN}}, // 前棱 (z=MIN) x方向
                {{MIN, MIN, MAX}, {MAX, MIN, MAX}}, // 后棱 (z=MAX) x方向
                {{MIN, MIN, MIN}, {MIN, MIN, MAX}}, // 左棱 (x=MIN) z方向
                {{MAX, MIN, MIN}, {MAX, MIN, MAX}}, // 右棱 (x=MAX) z方向
                
                // 顶部4条 (y=MAX)
                {{MIN, MAX, MIN}, {MAX, MAX, MIN}}, // 前棱
                {{MIN, MAX, MAX}, {MAX, MAX, MAX}}, // 后棱
                {{MIN, MAX, MIN}, {MIN, MAX, MAX}}, // 左棱
                {{MAX, MAX, MIN}, {MAX, MAX, MAX}}, // 右棱
                
                // 垂直4条 (连接底顶)
                {{MIN, MIN, MIN}, {MIN, MAX, MIN}}, // 左前
                {{MAX, MIN, MIN}, {MAX, MAX, MIN}}, // 右前
                {{MIN, MIN, MAX}, {MIN, MAX, MAX}}, // 左后
                {{MAX, MIN, MAX}, {MAX, MAX, MAX}}  // 右后
            };

            // 每条棱生成1个粒子，位置在棱上随机分布
            for (double[][] edge : edges) {
                double[] start = edge[0];
                double[] end = edge[1];
                double t = Math.random(); // 0.0 ~ 1.0 随机插值比例

                // 线性插值计算随机位置: point = start + t * (end - start)
                double x = pos.getX() + start[0] + t * (end[0] - start[0]);
                double y = pos.getY() + start[1] + t * (end[1] - start[1]);
                double z = pos.getZ() + start[2] + t * (end[2] - start[2]);

                serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x, y, z,
                    1,        // 每棱1个粒子
                    0.03,     // 略微增加扩散增强随机感
                    0.03,
                    0.03,
                    0.015     // 略微提升上升速度
                );
            }
        }
    }
    private static void auricOreDamage(LevelAccessor world, Entity entity) {
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