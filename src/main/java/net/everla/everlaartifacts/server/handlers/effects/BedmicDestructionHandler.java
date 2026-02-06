package net.everla.everlaartifacts.server.handlers.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class BedmicDestructionHandler {

    // 缓存上一次处理的时间，避免过于频繁的操作
    private static final java.util.WeakHashMap<LivingEntity, Long> lastProcessTime = new java.util.WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        
        // 检查玩家是否具有BedmicDestruction状态效果
        if (hasBedmicDestructionEffect(player)) {
            // 控制执行频率，避免每tick都执行
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastProcessTime.get(player);
            if (lastTime == null || currentTime - lastTime > 1000) { // 每秒最多执行一次
                destroyBlocksAroundPlayer(player);
                lastProcessTime.put(player, currentTime);
            }
        }
    }

    private static boolean hasBedmicDestructionEffect(Player player) {
        // 检查玩家是否具有BedmicDestruction效果
        MobEffectInstance effect = player.getEffect(net.everla.everlaartifacts.init.EverlaartifactsModMobEffects.BEDMIC_DESTRUCTION.get());
        return effect != null;
    }

    private static void destroyBlocksAroundPlayer(Player player) {
        Level level = player.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        int radius = 10;

        // 计算有效的Y轴范围，考虑世界的最大最小高度
        int minY = Math.max(level.getMinBuildHeight(), playerPos.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, playerPos.getY() + radius);

        // 创建标签键来检测特定方块
        TagKey<Block> targetTag = TagKey.create(Registries.BLOCK, 
            ResourceLocation.fromNamespaceAndPath("everlatweaker", "can_destroyed_by_weird_cocktail"));

        // 遍历玩家周围10格范围内的方块
        List<BlockPos> blocksToDestroy = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = minY - playerPos.getY(); y <= maxY - playerPos.getY(); y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    
                    // 检查坐标是否在有效范围内
                    if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
                        BlockState blockState = level.getBlockState(pos);
                        
                        // 检查方块是否属于指定的标签，并且满足part=foot的条件
                        if (blockState.is(targetTag) && hasPartFootProperty(blockState)) {
                            
                            // 确保不是玩家所在位置，避免误伤
                            if (!pos.equals(player.blockPosition()) && 
                                !pos.equals(player.blockPosition().above()) && 
                                !pos.equals(player.blockPosition().below())) {
                                
                                blocksToDestroy.add(pos.immutable());
                            }
                        }
                    }
                }
            }
        }

        // 批量销毁方块以提高性能
        for (BlockPos pos : blocksToDestroy) {
            if (level.getBlockState(pos).getBlock() != net.minecraft.world.level.block.Blocks.AIR) {
                serverLevel.destroyBlock(pos, true); // true表示掉落物品
            }
        }
    }

    // 检查方块状态是否具有part=foot属性
    private static boolean hasPartFootProperty(BlockState state) {
        // 获取所有属性
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("part")) {
                // 检查属性值是否为"foot"
                Comparable<?> value = state.getValue(property);
                return "foot".equals(value.toString());
            }
        }
        // 如果方块没有part属性，则假设它符合条件（根据原有命令行为）
        return true;
    }
}