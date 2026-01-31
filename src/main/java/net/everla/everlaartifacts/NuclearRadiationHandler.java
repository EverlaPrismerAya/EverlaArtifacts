package net.everla.everlaartifacts;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.init.EverlaartifactsModFluids;
import net.everla.everlaartifacts.fluid.NuclearWasteWaterFluid;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NuclearRadiationHandler {
    
    // Alex's Caves模组ID和irradiated状态效果的ResourceLocation
    private static final String ALEXS_CAVES_MODID = "alexscaves";
    private static final ResourceLocation IRRADIATED_EFFECT_LOCATION = ResourceLocation.fromNamespaceAndPath(ALEXS_CAVES_MODID, "irradiated");
    
    // 用于控制效果更新频率的计数器
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounter = new java.util.WeakHashMap<>();
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        
        // 检查是否加载了Alex's Caves模组
        if (!ModList.get().isLoaded(ALEXS_CAVES_MODID)) {
            return;
        }
        
        // 每5个tick执行一次，减少性能开销
        java.util.UUID playerUUID = player.getUUID();
        int currentTick = playerTickCounter.getOrDefault(playerUUID, 0) + 1;
        playerTickCounter.put(playerUUID, currentTick);
        
        if (currentTick % 5 != 0) { // 每5个tick执行一次
            return;
        }
        
        // 检查玩家是否拥有NuclearWaterRadiation状态效果
        boolean hasNuclearRadiation = player.hasEffect(EverlaartifactsModMobEffects.NUCLEAR_WATER_RADIATION.get());
        
        // 检查玩家是否站在NuclearWasteWater流体中
        boolean inNuclearWasteWater = isInNuclearWasteWater(player);
        
        // 如果满足条件，给予alexscaves:irradiated状态效果
        if (hasNuclearRadiation || inNuclearWasteWater) {
            // 获取Alex's Caves的irradiated状态效果
            MobEffect irradiatedEffect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(IRRADIATED_EFFECT_LOCATION);
            
            if (irradiatedEffect != null) {
                // 检查玩家是否已经拥有此效果且持续时间较长，避免不必要的重复添加
                MobEffectInstance existingEffect = player.getEffect(irradiatedEffect);
                if (existingEffect == null || existingEffect.getDuration() < 20) { // 如果效果不存在或剩余时间少于1秒
                    // 给予玩家2秒的irradiated效果，等级为0（基础等级）
                    player.addEffect(new MobEffectInstance(irradiatedEffect, 40, 0, false, true)); // 40 ticks = 2秒
                }
            }
        }
    }
    
    /**
     * 检查玩家是否站在NuclearWasteWater流体中
     */
    private static boolean isInNuclearWasteWater(Player player) {
        // 检查玩家脚下的方块位置
        BlockPos feetPos = player.getOnPos();
        BlockPos headPos = player.blockPosition();
        
        // 检查脚部和头部位置是否有核废水流体
        var level = player.level();
        var feetFluidState = level.getFluidState(feetPos);
        var headFluidState = level.getFluidState(headPos);
        
        // 检查流体类型是否为核废水
        boolean feetInNuclearWaste = feetFluidState.getType() == EverlaartifactsModFluids.NUCLEAR_WASTE_WATER.get() ||
                                    feetFluidState.getType() == EverlaartifactsModFluids.FLOWING_NUCLEAR_WASTE_WATER.get();
        boolean headInNuclearWaste = headFluidState.getType() == EverlaartifactsModFluids.NUCLEAR_WASTE_WATER.get() ||
                                    headFluidState.getType() == EverlaartifactsModFluids.FLOWING_NUCLEAR_WASTE_WATER.get();
        
        // 还需要检查玩家身体中间部分，所以检查下方一个方块
        BlockPos bodyPos = new BlockPos(headPos.getX(), (int)player.getY() - 1, headPos.getZ());
        var bodyFluidState = player.level().getFluidState(bodyPos);
        boolean bodyInNuclearWaste = bodyFluidState.getType() == EverlaartifactsModFluids.NUCLEAR_WASTE_WATER.get() ||
                                    bodyFluidState.getType() == EverlaartifactsModFluids.FLOWING_NUCLEAR_WASTE_WATER.get();
        
        return feetInNuclearWaste || headInNuclearWaste || bodyInNuclearWaste;
    }
}