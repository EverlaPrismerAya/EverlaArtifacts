package net.everla.everlaartifacts.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.everla.everlaartifacts.procedures.MakeEntityFlyProcedure;

public class DeepslateAuricOreBlock extends Block {
    // 静态冷却计数器：防止同一 tick 内重复生成粒子
    private static int lastParticleTick = -1;

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
        MakeEntityFlyProcedure.execute(world, entity);

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
}