package net.everla.everlaartifacts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TPAuraHandler {
    
    private static final Random RANDOM = new Random();
    private static final int MAX_RADIUS = 10;
    private static final TargetingConditions TARGET_CONDITIONS = TargetingConditions.forCombat()
            .range(MAX_RADIUS)
            .ignoreLineOfSight();
    
    // 追踪每个玩家的TPAura使用状态
    private static final java.util.Map<Player, Boolean> isUsingTPAura = new WeakHashMap<>();
    
    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.level();
        
        if (level.isClientSide()) {
            return;
        }
        
        ItemStack mainHandItem = player.getMainHandItem();
        int enchantmentLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(
                EverlaartifactsModEnchantments.TP_AURA.get(), mainHandItem);
        
        if (enchantmentLevel > 0) {
            // 右键点击时触发TPAura效果
            triggerTPAuraAbility(player, mainHandItem, (ServerLevel) level);
            isUsingTPAura.put(player, true);
            
            // 阻止正常的右键交互行为，因为我们只想触发TPAura效果
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        
        Player player = event.player;
        Level level = player.level();
        
        if (level.isClientSide()) {
            return;
        }
        
        // 检查主手是否持有带TPAura附魔的物品
        ItemStack mainHandItem = player.getMainHandItem();
        int enchantmentLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(
                EverlaartifactsModEnchantments.TP_AURA.get(), mainHandItem);
        
        // 当玩家不再持有TPAura附魔物品时，重置状态
        if (enchantmentLevel == 0) {
            isUsingTPAura.put(player, false);
        }
    }
    
    private static void triggerTPAuraAbility(Player player, ItemStack itemStack, ServerLevel level) {
        // 寻找10格半径内的生物
        AABB searchBox = player.getBoundingBox().inflate(MAX_RADIUS);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox, 
                mob -> !mob.getUUID().equals(player.getUUID()) && !mob.isSpectator() && TARGET_CONDITIONS.test(player, mob));
        
        if (nearbyMobs.isEmpty()) {
            return; // 没有找到可攻击的目标
        }
        
        // 随机选择一个目标
        Mob target = nearbyMobs.get(RANDOM.nextInt(nearbyMobs.size()));
        
        // 计算玩家到目标的方向向量
        Vec3 targetPosition = target.position();
        Vec3 playerPosition = player.position();
        Vec3 directionToTarget = targetPosition.subtract(playerPosition).normalize();
        
        // 计算目标背后2格的位置（相对于目标的方向），模仿外挂的特性
        Vec3 behindTarget = targetPosition.subtract(directionToTarget.multiply(2.0, 0.0, 2.0));
        
        // 尝试找到一个安全的传送位置
        BlockPos teleportPos = findSafeTeleportPosition(level, behindTarget, target);
        
        if (teleportPos != null) {
            // 传送玩家到目标背后2格
            player.teleportTo(teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5);
            
            // 触发一次攻击事件，这样其他附魔效果就能正常工作
            AttackEntityEvent attackEvent = new AttackEntityEvent(player, target);
            MinecraftForge.EVENT_BUS.post(attackEvent);
            
            if (!attackEvent.isCanceled()) {
                // 对目标造成伤害（相当于玩家攻击力，并应用所有附魔效果）
                float baseAttackDamage = (float) player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue();
                
                // 使用标准的玩家攻击方式来确保所有附魔都被应用
                DamageSource damageSource = level.damageSources().playerAttack(player);
                
                // 先应用附魔效果，再造成伤害
                EnchantmentHelper.doPostHurtEffects(target, player);
                EnchantmentHelper.doPostDamageEffects(player, target);
                
                // 对目标造成伤害
                boolean attacked = target.hurt(damageSource, baseAttackDamage);
                
                if (attacked) {
                    // 如果攻击成功，处理攻击后效果
                    if (target instanceof LivingEntity) {
                        EnchantmentHelper.doPostHurtEffects((LivingEntity) target, player);
                    }
                }
            }
            
            // 减少物品耐久
            itemStack.hurtAndBreak(1, player, (p) -> {
                p.broadcastBreakEvent(InteractionHand.MAIN_HAND);
            });
            
            // 模拟攻击动画 - 挥动物品（在客户端和服务端都需要）
            player.swing(InteractionHand.MAIN_HAND);
            
            // 移除了粒子效果，因为您要求不要添加
        }
    }
    
    private static BlockPos findSafeTeleportPosition(ServerLevel level, Vec3 desiredPos, Entity target) {
        // 获取期望位置的坐标
        int originX = Mth.floor(desiredPos.x);
        int originY = Mth.floor(desiredPos.y);
        int originZ = Mth.floor(desiredPos.z);
        
        // 首先尝试原始位置
        BlockPos originPos = new BlockPos(originX, originY, originZ);
        if (isSafeStandablePosition(level, originPos, target)) {
            return originPos;
        }
        
        // 如果原始位置不安全，尝试周围的其他位置
        // 搜索半径为2的立方体区域
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos candidatePos = new BlockPos(originX + dx, originY + dy, originZ + dz);
                    
                    if (isSafeStandablePosition(level, candidatePos, target)) {
                        return candidatePos;
                    }
                }
            }
        }
        
        // 如果在附近找不到安全位置，返回null
        return null;
    }
    
    private static boolean isSafeStandablePosition(ServerLevel level, BlockPos pos, Entity entity) {
        // 检查脚部位置是否为固体方块
        if (!level.getBlockState(pos).blocksMotion()) {
            // 如果脚下是空气，检查下方是否为固体
            BlockPos belowPos = pos.below();
            if (!level.getBlockState(belowPos).blocksMotion()) {
                return false; // 下方也是空气，无法站立
            }
        }
        
        // 检查身体位置（当前位置）是否为空气
        if (level.getBlockState(pos).blocksMotion()) {
            return false; // 身体位置是固体方块，无法传送
        }
        
        // 检查头部位置（上方）是否为空气
        BlockPos abovePos = pos.above();
        if (level.getBlockState(abovePos).blocksMotion()) {
            return false; // 头部位置是固体方块，无法传送
        }
        
        // 检查该位置是否会导致实体卡在方块中
        AABB entityBounds = entity.getDimensions(entity.getPose()).makeBoundingBox(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        
        return level.noCollision(entity, entityBounds);
    }
}