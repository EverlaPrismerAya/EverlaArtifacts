package net.everla.everlaartifacts;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class SteadfastHandler {
    // ===== 核心修复：移除固定数组，改用线性公式计算 =====
    // 增伤公式：5 + (level - 1) * 3 = 2 + level * 3
    //   level 1: 2 + 1*3 = 5
    //   level 2: 2 + 2*3 = 8
    //   level 3: 2 + 3*3 = 11
    //   level 4: 2 + 4*3 = 14（超限继续增长）
    private static double getAttackDamageBonus(int level) {
        return 2.0 + level * 3.0;
    }
    
    // 突进伤害公式：10 + (level - 1) * 2 = 8 + level * 2
    //   level 1: 8 + 1*2 = 10
    //   level 2: 8 + 2*2 = 12
    //   level 3: 8 + 3*2 = 14
    //   level 4: 8 + 4*2 = 16（超限继续增长）
    private static double getDashDamage(int level) {
        return 8.0 + level * 2.0;
    }
    
    // 击倒效果参数（1.2秒 = 24 tick）
    private static final int KNOCKDOWN_DURATION = 24;
    
    // 玩家级击倒冷却（每1.5秒一次）
    private static final Map<UUID, Long> PLAYER_KNOCKDOWN_COOLDOWN = new HashMap<>();
    private static final long KNOCKDOWN_COOLDOWN_TICKS = 30L; // 1.5秒
    
    // 属性修饰符UUID
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");
    
    // 击倒状态跟踪
    private static final Map<UUID, Integer> KNOCKDOWN_REMAINING_TICKS = new HashMap<>();
    
    // 突进冷却
    private static final Map<UUID, Long> LAST_DASH_TIME = new HashMap<>();
    private static final long DASH_COOLDOWN_TICKS = 40L; // 2秒

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) return;
        
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        
        LivingEntity target = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        int levelEnch = getSteadfastLevel(mainHand);
        if (levelEnch <= 0) return;
        
        // ===== 核心修复：使用公式计算增伤（支持超限等级）=====
        float attackStrength = player.getAttackStrengthScale(0.5f);
        double baseBonus = getAttackDamageBonus(levelEnch); // 线性公式
        double actualBonus = baseBonus * attackStrength;
        event.setAmount(event.getAmount() + (float) actualBonus);
        
        // 击倒效果（1.5秒冷却）
        UUID playerId = player.getUUID();
        long currentTime = level.getGameTime();
        
        if (PLAYER_KNOCKDOWN_COOLDOWN.containsKey(playerId) && 
            currentTime - PLAYER_KNOCKDOWN_COOLDOWN.get(playerId) < KNOCKDOWN_COOLDOWN_TICKS) {
            return;
        }
        
        applyKnockdown(target, (ServerLevel) level);
        PLAYER_KNOCKDOWN_COOLDOWN.put(playerId, currentTime);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide() || !player.isCrouching()) {
            return;
        }
        
        ItemStack handItem = player.getItemInHand(event.getHand());
        int levelEnch = getSteadfastLevel(handItem);
        if (levelEnch <= 0) return;
        
        UUID playerId = player.getUUID();
        long currentTime = level.getGameTime();
        if (LAST_DASH_TIME.containsKey(playerId) && 
            currentTime - LAST_DASH_TIME.get(playerId) < DASH_COOLDOWN_TICKS) {
            return;
        }
        
        // ===== 核心修复：使用公式计算突进伤害（支持超限等级）=====
        float attackStrength = player.getAttackStrengthScale(0.5f);
        double baseDashDamage = getDashDamage(levelEnch); // 线性公式
        performDash(player, baseDashDamage, attackStrength, (ServerLevel) level);
        
        LAST_DASH_TIME.put(playerId, currentTime);
        handItem.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
    }

    private static void performDash(Player player, double baseDashDamage, float attackStrength, ServerLevel level) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 end = start.add(look.scale(5.0));
        
        AABB rayAABB = new AABB(start, end).inflate(0.75);
        List<LivingEntity> entities = level.getEntitiesOfClass(
            LivingEntity.class, 
            rayAABB,
            entity -> entity != player && entity.isAlive()
        );
        
        // 应用攻击冷却衰减
        double actualDamage = baseDashDamage * attackStrength;
        
        for (LivingEntity entity : entities) {
            Vec3 entityPos = entity.position();
            double distToStart = entityPos.distanceToSqr(start);
            double distToEnd = entityPos.distanceToSqr(end);
            double rayLength = start.distanceToSqr(end);
            
            if (distToStart + distToEnd - rayLength > 4.0) continue;
            
            entity.hurt(level.damageSources().playerAttack(player), (float) actualDamage);
        }
        
        // 执行突进
        double speed = 3.0;
        player.setDeltaMovement(look.x * speed, look.y * speed * 0.2, look.z * speed);
        player.hurtMarked = true;
        
        // 粒子效果
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, 
            player.getX(), player.getY() + 1.0, player.getZ(), 
            15, 0.8, 0.8, 0.8, 0.1);
    }

    private static void applyKnockdown(LivingEntity entity, ServerLevel level) {
        // 移速归零
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_UUID);
            speedAttr.addTransientModifier(new AttributeModifier(
                SPEED_MODIFIER_UUID,
                "steadfast_knockdown_speed",
                -1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
        
        // 攻击力归零
        AttributeInstance damageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.removeModifier(DAMAGE_MODIFIER_UUID);
            damageAttr.addTransientModifier(new AttributeModifier(
                DAMAGE_MODIFIER_UUID,
                "steadfast_knockdown_damage",
                -1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
        
        KNOCKDOWN_REMAINING_TICKS.put(entity.getUUID(), KNOCKDOWN_DURATION);
        
        // 视觉反馈
        level.sendParticles(
            ParticleTypes.CLOUD,
            entity.getX(), 
            entity.getY() + entity.getBbHeight() * 0.4, 
            entity.getZ(),
            12,
            0.5,
            0.5,
            0.5,
            0.04
        );
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        
        long currentTime = event.getServer().getTickCount();
        
        // 清理击倒状态
        Iterator<Map.Entry<UUID, Integer>> iterator = KNOCKDOWN_REMAINING_TICKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            UUID entityUUID = entry.getKey();
            int remaining = entry.getValue() - 1;
            
            if (remaining <= 0) {
                LivingEntity entity = findEntityByUUID(event.getServer(), entityUUID);
                if (entity != null) {
                    AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speedAttr != null) {
                        speedAttr.removeModifier(SPEED_MODIFIER_UUID);
                    }
                    
                    AttributeInstance damageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (damageAttr != null) {
                        damageAttr.removeModifier(DAMAGE_MODIFIER_UUID);
                    }
                }
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
        
        // 清理冷却数据
        PLAYER_KNOCKDOWN_COOLDOWN.entrySet().removeIf(e -> 
            currentTime - e.getValue() > KNOCKDOWN_COOLDOWN_TICKS + 20);
        LAST_DASH_TIME.entrySet().removeIf(e -> 
            currentTime - e.getValue() > 100);
    }
    
    private static LivingEntity findEntityByUUID(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
                return livingEntity;
            }
        }
        return null;
    }

    private static int getSteadfastLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return EnchantmentHelper.getTagEnchantmentLevel(
            EverlaartifactsModEnchantments.STEADFAST.get(), 
            stack
        );
    }
}