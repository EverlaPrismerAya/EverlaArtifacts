package net.everla.everlaartifacts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TPAuraHandler {
    
    private static final Random RANDOM = new Random();
    private static final int MAX_RADIUS = 12; // 扩大检测范围到直径12（半径6）
    private static final TargetingConditions TARGET_CONDITIONS = TargetingConditions.forCombat()
            .range(MAX_RADIUS)
            .ignoreLineOfSight();
    
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
            
            // 阻止正常的右键交互行为，因为我们只想触发TPAura效果
            event.setCanceled(true);
        }
    }
    
    private static void triggerTPAuraAbility(Player player, ItemStack itemStack, ServerLevel level) {
        // 寻找12格半径内的生物
        AABB searchBox = player.getBoundingBox().inflate(MAX_RADIUS);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox, 
                mob -> !mob.getUUID().equals(player.getUUID()) && !mob.isSpectator() && TARGET_CONDITIONS.test(player, mob));
        
        if (nearbyMobs.isEmpty()) {
            // 没有找到目标，直接返回
            return; 
        }
        
        // 随机选择一个目标
        Mob target = nearbyMobs.get(RANDOM.nextInt(nearbyMobs.size()));
        
        // 计算目标位置
        Vec3 targetPosition = target.position();
        
        // 计算一个随机方向，距离目标3.5方块的位置
        double angle = RANDOM.nextDouble() * 2 * Math.PI; // 随机角度
        double distance = 3.5; // 固定距离3.5方块
        
        // 计算新的X和Z坐标
        double newX = targetPosition.x + distance * Math.cos(angle);
        double newZ = targetPosition.z + distance * Math.sin(angle);
        double newY = targetPosition.y; // 保持相同高度
        
        Vec3 randomPos = new Vec3(newX, newY, newZ);
        
        // 检查目标是否在空中
        BlockPos targetBlockPos = new BlockPos((int) targetPosition.x, (int) (targetPosition.y - 1), (int) targetPosition.z);
        boolean isTargetInAir = level.getBlockState(targetBlockPos).isAir();
        
        BlockPos teleportPos;
        if (isTargetInAir) {
            // 如果目标在空中，直接传送到目标附近的空中位置
            teleportPos = new BlockPos((int) randomPos.x, (int) randomPos.y, (int) randomPos.z);
            // 重置玩家的下落高度
            player.fallDistance = 0.0F;
        } else {
            // 使用简化的传送检查（只检查防穿墙）
            teleportPos = findSimpleTeleportPosition(level, randomPos, target);
        }
        
        if (teleportPos != null) {
            // 传送玩家到目标周围3.5方块的随机位置
            player.teleportTo(teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5);
            
            // 重要：无论玩家是否在地上，都要触发攻击
            // 创建一个攻击事件，让其他附魔正常工作
            AttackEntityEvent attackEvent = new AttackEntityEvent(player, target);
            MinecraftForge.EVENT_BUS.post(attackEvent);
            
            if (!attackEvent.isCanceled()) {
                // 应用所有硬编码的附魔效果
                boolean attacked = applyHardcodedEnchantments(player, itemStack, target, level);
                
                // 如果攻击成功，生成横扫粒子效果并播放音效
                if (attacked) {
                    // 播放横扫攻击音效
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), 
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    
                    // 生成横扫粒子效果
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                            target.getX(), target.getY() + 0.5, target.getZ(),
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            
            // 减少物品耐久
            itemStack.hurtAndBreak(1, player, (p) -> {
                p.broadcastBreakEvent(InteractionHand.MAIN_HAND);
            });
        } else {
            // 即使传送失败，也要尝试攻击（如果目标足够近）
            // 检查玩家与目标的距离
            double playerTargetDistance = player.distanceTo(target);
            if (playerTargetDistance <= 3.0) { // 如果距离足够近，直接攻击
                // 创建一个攻击事件，让其他附魔正常工作
                AttackEntityEvent attackEvent = new AttackEntityEvent(player, target);
                MinecraftForge.EVENT_BUS.post(attackEvent);
                
                if (!attackEvent.isCanceled()) {
                    // 应用所有硬编码的附魔效果
                    boolean attacked = applyHardcodedEnchantments(player, itemStack, target, level);
                    
                    // 如果攻击成功，生成横扫粒子效果并播放音效
                    if (attacked) {
                        // 播放横扫攻击音效
                        level.playSound(null, target.getX(), target.getY(), target.getZ(), 
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
                        
                        // 生成横扫粒子效果
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                                target.getX(), target.getY() + 0.5, target.getZ(),
                                1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                
                // 减少物品耐久
                itemStack.hurtAndBreak(1, player, (p) -> {
                    p.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                });
            }
        }
    }
    
    private static boolean applyHardcodedEnchantments(Player player, ItemStack itemStack, Entity target, ServerLevel level) {
        // 获取各种附魔等级
        int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, itemStack);
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, itemStack); // 锋利度
        int baneOfArthropodsLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, itemStack); // 节肢杀手
        int smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, itemStack); // 亡灵杀手
        int knockbackLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, itemStack); // 击退
        
        // 计算基础伤害
        float baseDamage = (float) player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue();
        
        // 应用锋利度附魔（对所有生物都有额外伤害）
        if (sharpnessLevel > 0) {
            baseDamage += (1 + sharpnessLevel * 0.5f); // 锋利度每级增加0.5点伤害
        }
        
        // 如果目标是节肢动物（蜘蛛、洞穴蜘蛛、末影螨），应用节肢杀手
        if (target instanceof net.minecraft.world.entity.monster.Spider ||
            target instanceof net.minecraft.world.entity.monster.CaveSpider ||
            target instanceof net.minecraft.world.entity.monster.Endermite) {
            if (baneOfArthropodsLevel > 0) {
                baseDamage += (1 + baneOfArthropodsLevel * 1.5f); // 节肢杀手对节肢动物有额外伤害
                // 对节肢动物造成缓慢效果
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * baneOfArthropodsLevel, 2));
                }
            }
        }
        
        // 如果目标是亡灵生物（骷髅、凋灵骷髅、僵尸等），应用亡灵杀手
        if (target instanceof net.minecraft.world.entity.monster.Skeleton ||
            target instanceof net.minecraft.world.entity.monster.Stray ||
            target instanceof net.minecraft.world.entity.monster.WitherSkeleton ||
            target instanceof net.minecraft.world.entity.monster.Zombie ||
            target instanceof net.minecraft.world.entity.monster.ZombieVillager ||
            target instanceof net.minecraft.world.entity.monster.Husk ||
            target instanceof net.minecraft.world.entity.monster.Drowned ||
            target instanceof net.minecraft.world.entity.raid.Raider) {
            if (smiteLevel > 0) {
                baseDamage += (1 + smiteLevel * 2.5f); // 亡灵杀手对亡灵生物有大量额外伤害
            }
        }
        
        // 应用击退附魔
        if (knockbackLevel > 0 && target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity) target;
            Vec3 playerLook = player.getLookAngle();
            livingTarget.push(-playerLook.x * knockbackLevel * 0.4, 0.4, -playerLook.z * knockbackLevel * 0.4);
        }
        
        // 应用火焰附加附魔
        if (fireAspectLevel > 0 && target instanceof LivingEntity) {
            target.setSecondsOnFire(fireAspectLevel * 4); // 每级火焰附加延长4秒燃烧时间
        }
        
        // 造成最终伤害
        DamageSource damageSource = level.damageSources().playerAttack(player);
        boolean attacked = false;
        if (target instanceof LivingEntity) {
            attacked = ((LivingEntity) target).hurt(damageSource, baseDamage);
        } else {
            attacked = target.hurt(damageSource, baseDamage);
        }
        
        return attacked; // 返回攻击是否成功
    }
    
    private static BlockPos findSimpleTeleportPosition(ServerLevel level, Vec3 desiredPos, Entity target) {
        // 获取期望位置的坐标
        int originX = Mth.floor(desiredPos.x);
        int originY = Mth.floor(desiredPos.y);
        int originZ = Mth.floor(desiredPos.z);
        
        // 首先尝试原始位置
        BlockPos originPos = new BlockPos(originX, originY, originZ);
        if (isSimpleTeleportSafe(level, originPos, target)) {
            return originPos;
        }
        
        // 如果原始位置不安全，尝试周围的其他位置
        // 搜索半径为2的立方体区域，只检查防穿墙
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos candidatePos = new BlockPos(originX + dx, originY + dy, originZ + dz);
                    
                    if (isSimpleTeleportSafe(level, candidatePos, target)) {
                        return candidatePos;
                    }
                }
            }
        }
        
        // 如果在附近找不到安全位置，返回null
        return null;
    }
    
    private static boolean isSimpleTeleportSafe(ServerLevel level, BlockPos pos, Entity entity) {
        // 检查该位置是否会导致实体卡在方块中（防穿墙检查）
        AABB entityBounds = entity.getDimensions(entity.getPose()).makeBoundingBox(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        
        // 确保传送位置不会导致实体卡在方块中
        return level.noCollision(entity, entityBounds);
    }
}