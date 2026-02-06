package net.everla.everlaartifacts.server.handlers.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber
public class BedmicDestructionHandler {

    private static final Random random = new Random();

    @SubscribeEvent
    public static void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
        // 确保在服务端执行
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        BlockPos bedPos = event.getPos();
        
        // 检查玩家是否具有BedmicDestruction状态效果
        if (hasBedmicDestructionEffect(player)) {
            Level level = player.level();
            
            // 在床的位置周围创建破坏区域
            if (level instanceof ServerLevel serverLevel) {
                // 创建真正的爆炸（强度为2）
                serverLevel.explode(
                    player, // 爆炸源为玩家
                    bedPos.getX() + 0.5,
                    bedPos.getY() + 0.5,
                    bedPos.getZ() + 0.5,
                    2.0F, // 爆炸强度为2
                    Level.ExplosionInteraction.BLOCK // 破坏方块
                );

                // 播放震撼的DELTARUNE爆炸音效
                serverLevel.playSound(
                    null,
                    bedPos,
                    net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                        ResourceLocation.fromNamespaceAndPath("everlaartifacts", "deltarune_explosion")
                    ),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    2.0F, // 进一步增强音量
                    1.0F  // 音调
                );
                
                // 添加大量火花粒子增加视觉冲击力
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CRIT,
                    bedPos.getX() + 0.5,
                    bedPos.getY() + 0.5,
                    bedPos.getZ() + 0.5,
                    80, // 增加火花粒子数量
                    1.5, 1.5, 1.5, // 扩大扩散范围
                    0.3 // 增加速度
                );
            }
        }
    }

    private static boolean hasBedmicDestructionEffect(Player player) {
        // 检查玩家是否具有BedmicDestruction效果
        MobEffectInstance effect = player.getEffect(net.everla.everlaartifacts.init.EverlaartifactsModMobEffects.BEDMIC_DESTRUCTION.get());
        return effect != null;
    }
}