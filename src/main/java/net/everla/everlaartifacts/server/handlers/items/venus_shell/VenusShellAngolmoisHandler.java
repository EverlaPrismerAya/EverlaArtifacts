package net.everla.everlaartifacts.server.handlers.items.venus_shell;

import net.everla.everlaartifacts.entity.AngolmoisDoomProjectileEntity;
import net.everla.everlaartifacts.init.EverlaartifactsModEntities;
import net.everla.everlaartifacts.item.VenusShellItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class VenusShellAngolmoisHandler {
    // 冷却时间：18秒 = 360 tick
    private static final long ANGOLMOIS_COOLDOWN = 360L;
    private static final String COOLDOWN_KEY = "everlaartifacts:venus_shell_angolmois_cooldown";
    
    // 伤害倍率：426%
    private static final float DAMAGE_MULTIPLIER = 4.26f;
    
    // 弹射物参数
    private static final int RANDOM_PROJECTILES = 5; // 弹射物B数量
    private static final int GUARANTEED_PROJECTILE = 1; // 弹射物A数量
    private static final double SPAWN_HEIGHT = 10.0; // 距离实体头顶10方块
    private static final double SPAWN_RADIUS = 5.0; // 半径5方块的随机位置
    private static final double EXPLOSION_RADIUS = 8.0; // 爆炸半径8方块
    
    // 冷却完成提示跟踪
    private static final Set<UUID> NOTIFIED_PLAYERS = new HashSet<>();
    
    // 用于控制冷却检查频率的计数器
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounter = new java.util.WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // 检查是否潜行且持有Venus Shell
        if (!player.isCrouching() || !(player.getMainHandItem().getItem() instanceof VenusShellItem)) {
            return;
        }
        
        // 检查冷却时间
        long currentTime = serverLevel.getGameTime();
        long lastUsed = player.getPersistentData().getLong(COOLDOWN_KEY);
        
        // 冷却中：允许普通攻击
        if (currentTime < lastUsed) {
            return;
        }
        
        // 冷却就绪：触发特殊攻击
        event.setCanceled(true);
        NOTIFIED_PLAYERS.remove(player.getUUID());
        executeAngolmoisAttack(player, serverLevel, event.getTarget());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        Level level = player.level();
        
        if (level.isClientSide() || !(player.getMainHandItem().getItem() instanceof VenusShellItem)) {
            return;
        }
        
        // 每10个tick检查一次冷却状态，减少性能开销
        UUID playerUUID = player.getUUID();
        int currentTick = playerTickCounter.getOrDefault(playerUUID, 0) + 1;
        playerTickCounter.put(playerUUID, currentTick);
        
        if (currentTick % 10 != 0) { // 每10个tick检查一次
            return;
        }
        
        long currentTime = level.getGameTime();
        long cooldownEnd = player.getPersistentData().getLong(COOLDOWN_KEY);
        
        if (currentTime >= cooldownEnd && !NOTIFIED_PLAYERS.contains(player.getUUID())) {
            sendCooldownReadyActionBar(player);
            NOTIFIED_PLAYERS.add(player.getUUID());
        }
    }

    private static void sendCooldownReadyActionBar(Player player) {
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("item.everlaartifacts.venus_shell.angolmois_ready"),
            true
        );
    }

    private static void executeAngolmoisAttack(Player player, ServerLevel level, Entity target) {
        // 获取玩家护甲值
        double armorValue = getPlayerArmorValue(player);
        
        // 在目标头顶生成弹射物
        spawnAngolmoisProjectiles(player, level, target, armorValue);
        
        // 设置冷却时间
        player.getPersistentData().putLong(COOLDOWN_KEY, level.getGameTime() + ANGOLMOIS_COOLDOWN);
        
        // 播放音效
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private static double getPlayerArmorValue(Player player) {
        var armorAttribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        return armorAttribute != null ? Math.max(armorAttribute.getValue(), 1.0) : 1.0;
    }

    private static void spawnAngolmoisProjectiles(Player player, ServerLevel level, Entity target, double armorValue) {
        Vec3 targetPos = target.position();
        double targetHeight = target.getBbHeight();
        
        // 生成5枚随机位置的弹射物B（落地无效果）
        for (int i = 0; i < RANDOM_PROJECTILES; i++) {
            Vec3 spawnPos = getRandomSpawnPosition(level, targetPos, targetHeight);
            spawnAngolmoisProjectile(level, player, spawnPos, targetPos, armorValue, false);
        }
        
        // 生成1枚固定位置的弹射物A（落地产生爆炸）
        Vec3 guaranteedSpawnPos = new Vec3(targetPos.x, targetPos.y + targetHeight + SPAWN_HEIGHT, targetPos.z);
        spawnAngolmoisProjectile(level, player, guaranteedSpawnPos, targetPos, armorValue, true);
    }

    private static Vec3 getRandomSpawnPosition(ServerLevel level, Vec3 targetPos, double targetHeight) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double radius = level.random.nextDouble() * SPAWN_RADIUS;
        double x = targetPos.x + Math.cos(angle) * radius;
        double z = targetPos.z + Math.sin(angle) * radius;
        double y = targetPos.y + targetHeight + SPAWN_HEIGHT;
        return new Vec3(x, y, z);
    }

    private static void spawnAngolmoisProjectile(ServerLevel level, Player player, Vec3 spawnPos, Vec3 targetPos, double armorValue, boolean isExplosive) {
        // 创建弹射物
        AngolmoisDoomProjectileEntity projectile = new AngolmoisDoomProjectileEntity(
            EverlaartifactsModEntities.ANGOLMOIS_DOOM_PROJECTILE.get(), 
            level
        );
        
        // 设置弹射物位置
        projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        
        // 计算朝向目标的轨迹
        Vec3 direction = targetPos.subtract(spawnPos).normalize();
        projectile.shoot(direction.x, direction.y, direction.z, 1.5f, 0);
        
        // 设置弹射物属性
        projectile.setOwner(player);
        projectile.setSilent(true);
        projectile.setBaseDamage(0.1); // 基础伤害很低，主要靠爆炸
        
        // 存储是否为爆炸弹射物的标记
        projectile.getPersistentData().putBoolean("IsExplosiveAngolmois", isExplosive);
        projectile.getPersistentData().putDouble("ExplosionDamage", armorValue * DAMAGE_MULTIPLIER);
        
        level.addFreshEntity(projectile);
    }

    /**
     * 处理弹射物落地爆炸效果
     */
    public static void handleAngolmoisExplosion(ServerLevel level, AngolmoisDoomProjectileEntity projectile, Vec3 explosionPos) {
        // 检查是否为爆炸弹射物
        if (!projectile.getPersistentData().getBoolean("IsExplosiveAngolmois")) {
            return; // 非爆炸弹射物，不产生效果
        }
        
        // 获取爆炸伤害值
        double damage = projectile.getPersistentData().getDouble("ExplosionDamage");
        Entity owner = projectile.getOwner();
        
        // 查找爆炸范围内的实体
        AABB explosionBox = new AABB(
            explosionPos.x - EXPLOSION_RADIUS, explosionPos.y - EXPLOSION_RADIUS, explosionPos.z - EXPLOSION_RADIUS,
            explosionPos.x + EXPLOSION_RADIUS, explosionPos.y + EXPLOSION_RADIUS, explosionPos.z + EXPLOSION_RADIUS
        );
        
        java.util.List<LivingEntity> entities = level.getEntitiesOfClass(
            LivingEntity.class,
            explosionBox,
            entity -> entity.isAlive() && !entity.isInvulnerable() && entity != owner
        );
        
        // 对范围内实体造成伤害
        for (LivingEntity entity : entities) {
            // 使用魔法伤害源
            var damageSource = owner instanceof Player player ? 
                player.damageSources().indirectMagic(projectile, player) : 
                level.damageSources().magic();
            
            // 绕过无敌帧
            int originalInvulnerableTime = entity.invulnerableTime;
            entity.invulnerableTime = 0;
            
            // 造成伤害
            entity.hurt(damageSource, (float) damage);
            
            // 恢复无敌帧时间
            entity.invulnerableTime = originalInvulnerableTime;
        }
        
        // 生成爆炸粒子效果
        level.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            explosionPos.x, explosionPos.y, explosionPos.z,
            1, 0, 0, 0, 0
        );
        
        level.sendParticles(
            ParticleTypes.SMOKE,
            explosionPos.x, explosionPos.y, explosionPos.z,
            50, 1.0, 1.0, 1.0, 0.1
        );
    }
}