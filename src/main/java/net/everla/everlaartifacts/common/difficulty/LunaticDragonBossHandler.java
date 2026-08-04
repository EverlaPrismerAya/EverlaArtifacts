package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;

import java.util.*;

/**
 * 末影龙Boss处理器
 * 当月狂或额外模式开启时（游戏难度为困难且enableLunaticMode游戏规则为true），
 * 当末影龙附近200方块范围内存在有底座的末地水晶时，末影龙免疫任何伤害
 * 同时实现动态减伤与防秒杀机制
 */
@Mod.EventBusSubscriber
public class LunaticDragonBossHandler {
    
    // 检测范围半径（200方块）
    private static final double DETECTION_RADIUS = 200.0;
    
    // 动态减伤相关常量
    private static final int DPS_CALCULATION_WINDOW = 1200; // 60秒 = 1200 ticks
    private static final double MAX_DAMAGE_REDUCTION = 0.80; // 最大80%减伤
    private static final double ANTI_OHK_THRESHOLD = 0.3; // 防秒杀阈值：单次伤害不超过最大生命值的30%
    
    // DPS跟踪数据结构 - 使用Boss实体UUID+玩家UUID作为复合键
    private static final Map<String, List<DamageRecord>> entityPlayerDamageHistory = new HashMap<>();
    private static final Map<String, Long> entityPlayerLastDamageTime = new HashMap<>();
    
    // 清理计数器
    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 100; // 每100个tick清理一次
    
    // 水晶重生相关常量
    private static final double HEALTH_RESTORE_THRESHOLD = 0.3; // 30%生命值阈值
    private static final double MAIN_ISLAND_RADIUS = 100.0; // 主岛范围半径
    private static final BlockPos END_PORTAL_CENTER = new BlockPos(0, 64, 0); // 末地传送门中心坐标
    
    // NBT标签键
    private static final String CRYSTAL_POSITIONS_KEY = "StoredCrystalPositions";
    private static final String HAS_RESTORATION_OCCURRED_KEY = "HasRestorationOccurred";
    
    // 末影人仇恨系统相关
    // 移除了不再需要的目标追踪数据结构
    
    /**
     * 监听生物受伤事件，处理末影龙的伤害免疫、动态减伤、防秒杀和生命值恢复逻辑
     * 同时处理月狂或额外模式下末影人攻击末影龙目标的机制
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查受伤实体是否为末影龙
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        
        // 检查是否启用了月狂或额外模式且世界难度为困难
        if (dragon.level().getDifficulty() != Difficulty.HARD) {
            return;
        }
        
        GameRules gameRules = dragon.level().getGameRules();
        if (!gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE)) {
            return;
        }
        
        // 检查末影龙附近是否存在有底座的末地水晶
        if (hasNearbyEndCrystalsWithPedestal(dragon)) {
            // 取消伤害事件，使末影龙免疫所有伤害
            event.setCanceled(true);
            return;
        }
        
        // 处理动态减伤和防秒杀逻辑
        processDynamicDamageReduction(event, dragon);
        
        // 处理生命值恢复逻辑
        processHealthRestoration(dragon);
        
        // 注意：这里不需要记录末影龙受到的伤害
        // 末影人仇恨系统在单独的事件监听器中处理
    }
    
    /**
     * 监听末影人受攻击事件，处理月狂或额外模式下末影人免疫末影龙攻击
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEndermanAttack(LivingAttackEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查受伤实体是否为末影人
        if (!(event.getEntity() instanceof EnderMan)) {
            return;
        }
        
        // 检查是否启用了月狂或额外模式
        if (!isLunaticModeEnabled(event.getEntity().level())) {
            return;
        }
        
        // 检查攻击者是否为末影龙
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof EnderDragon) {
            // 取消攻击事件，使末影人在月狂或额外模式下完全免疫末影龙攻击
            // 这会阻止伤害、击退、闪红等所有攻击效果
            event.setCanceled(true);
            return;
        }
    }
    
    /**
     * 监听所有生物受伤事件，处理月狂或额外模式下末影人对末影龙攻击目标的仇恨转移
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityHurt(LivingHurtEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查是否启用了月狂或额外模式
        if (!isLunaticModeEnabled(event.getEntity().level())) {
            return;
        }
        
        // 检查攻击者是否为末影龙
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof EnderDragon dragon)) {
            return;
        }
        
        // 检查受伤实体是否在免疫列表中（排除特定实体）
        if (isEntityImmuneToEndermanAggression(event.getEntity())) {
            return;
        }
        
        // 末影龙攻击了非末影人目标，触发附近末影人的仇恨
        triggerEndermanAggression(dragon, event.getEntity());
    }
    
    /**
     * 检查末影龙附近是否存在有底座的末地水晶
     * 
     * @param dragon 末影龙实体
     * @return 如果存在有底座的末地水晶返回true，否则返回false
     */
    private static boolean hasNearbyEndCrystalsWithPedestal(EnderDragon dragon) {
        // 创建搜索区域包围盒
        AABB searchArea = dragon.getBoundingBox().inflate(DETECTION_RADIUS);
        
        // 在搜索区域内查找末地水晶
        return dragon.level().getEntitiesOfClass(EndCrystal.class, searchArea, crystal -> {
            // 检查末地水晶是否在末地传送门底座上
            return isEndCrystalOnPedestal(crystal);
        }).size() > 0;
    }
    

    /**
     * 触发附近末影人对末影龙目标的攻击
     * 
     * @param dragon 末影龙实体
     * @param target 被攻击的目标
     */
    private static void triggerEndermanAggression(EnderDragon dragon, Entity target) {
        // 确保目标是活体实体
        if (!(target instanceof net.minecraft.world.entity.LivingEntity livingTarget)) {
            return;
        }
        
        // 搜索附近的末影人（以被攻击目标为中心，32方块半径）
        AABB searchArea = livingTarget.getBoundingBox().inflate(32.0);
        List<EnderMan> nearbyEndermen = livingTarget.level().getEntitiesOfClass(
            EnderMan.class, searchArea);
            
        int triggeredCount = 0;
        for (EnderMan enderman : nearbyEndermen) {
            // 排除已经被激怒的末影人
            if (enderman.getTarget() != null) {
                continue;
            }
            
            // 检查末影人是否能看见目标
            if (!enderman.getSensing().hasLineOfSight(livingTarget)) {
                continue;
            }
                
            // 设置末影人的目标为末影龙攻击的目标
            enderman.setTarget(livingTarget);
            triggeredCount++;
        }
    }
        

    /**
     * 检查实体是否免疫末影人的协同攻击
     * 
     * @param entity 要检查的实体
     * @return 如果实体免疫末影人协同攻击返回true，否则返回false
     */
    private static boolean isEntityImmuneToEndermanAggression(Entity entity) {
        // 获取实体的注册名
        String entityName = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        
        // 检查是否在配置的免疫列表中
        return EverlaArtifactsConfig.getImmuneToEndermanAggressionSet().contains(entityName);
    }
        
    /**
     * 检查是否启用了月狂或额外模式
     * 
     * @param level 世界对象
     * @return 是否处于月狂或额外模式
     */
    private static boolean isLunaticModeEnabled(net.minecraft.world.level.Level level) {
        if (level.getDifficulty() != Difficulty.HARD) {
            return false;
        }
            
        GameRules gameRules = level.getGameRules();
        return gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE);
    }
        
    /**
     * 检查末地水晶是否位于末地传送门底座上
     * 
     * @param crystal 末地水晶实体
     * @return 如果末地水晶在底座上返回true，否则返回false
     */
    private static boolean isEndCrystalOnPedestal(EndCrystal crystal) {
        // 检查末地水晶是否显示底部（意味着它在底座上）
        return crystal.showsBottom();
    }
    
    /**
     * 处理动态减伤和防秒杀逻辑
     * 
     * @param event 伤害事件
     * @param dragon 末影龙实体
     */
    private static void processDynamicDamageReduction(LivingHurtEvent event, EnderDragon dragon) {
        DamageSource damageSource = event.getSource();
        Entity attacker = damageSource.getEntity();
        
        // 只处理玩家造成的伤害
        if (!(attacker instanceof Player player)) {
            return;
        }
        
        UUID playerUUID = player.getUUID();
        UUID entityUUID = dragon.getUUID();
        float originalDamage = event.getAmount();
        long currentTime = dragon.level().getGameTime();
        
        // 记录伤害历史（按实体隔离）
        recordPlayerDamage(entityUUID, playerUUID, originalDamage, currentTime);
        
        // 计算玩家当前DPS（按实体隔离）
        double currentDPS = calculatePlayerDPS(entityUUID, playerUUID, currentTime);
        
        // 计算目标有效DPS (总血量/120秒)
        double targetDPS = calculateTargetDPS(dragon);
        
        // 计算减伤率
        double reductionRate = calculateDamageReductionInternal(currentDPS, targetDPS);
        
        // 应用动态减伤
        float reducedDamage = (float) (originalDamage * (1.0 - reductionRate));
        
        // 应用防秒杀机制
        float antiOHKDamage = applyAntiOHK(reducedDamage, dragon);
        
        // 设置最终伤害值
        event.setAmount(antiOHKDamage);
        
        // 更新最后一次伤害时间（按实体隔离）
        entityPlayerLastDamageTime.put(getEntityPlayerKey(entityUUID, playerUUID), currentTime);
    }
    
    /**
     * 生成实体-玩家复合键
     * 
     * @param entityUUID 实体UUID
     * @param playerUUID 玩家UUID
     * @return 复合键字符串
     */
    private static String getEntityPlayerKey(UUID entityUUID, UUID playerUUID) {
        return entityUUID.toString() + ":" + playerUUID.toString();
    }
    
    /**
     * 记录玩家伤害历史（按实体隔离）
     * 
     * @param entityUUID 实体UUID
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     * @param timestamp 时间戳
     */
    private static void recordPlayerDamage(UUID entityUUID, UUID playerUUID, float damage, long timestamp) {
        String key = getEntityPlayerKey(entityUUID, playerUUID);
        entityPlayerDamageHistory.computeIfAbsent(key, k -> new ArrayList<>())
            .add(new DamageRecord(damage, timestamp));
    }
    
    /**
     * 计算玩家当前DPS（按实体隔离）
     * 
     * @param entityUUID 实体UUID
     * @param playerUUID 玩家UUID
     * @param currentTime 当前时间
     * @return 玩家当前DPS
     */
    private static double calculatePlayerDPS(UUID entityUUID, UUID playerUUID, long currentTime) {
        String key = getEntityPlayerKey(entityUUID, playerUUID);
        List<DamageRecord> damageRecords = entityPlayerDamageHistory.get(key);
        if (damageRecords == null || damageRecords.isEmpty()) {
            return 0.0;
        }
        
        // 清理过期记录
        damageRecords.removeIf(record -> currentTime - record.timestamp > DPS_CALCULATION_WINDOW);
        
        if (damageRecords.isEmpty()) {
            return 0.0;
        }
        
        // 计算窗口期内的总伤害
        double totalDamage = damageRecords.stream()
            .mapToDouble(record -> record.damage)
            .sum();
        
        // 计算实际时间窗口长度（ticks）
        long timeWindow = Math.min(DPS_CALCULATION_WINDOW, 
            currentTime - damageRecords.get(0).timestamp);
        
        // 转换为秒并计算DPS
        double timeInSeconds = timeWindow / 20.0;
        return timeInSeconds > 0 ? totalDamage / timeInSeconds : 0.0;
    }
    
    /**
     * 计算目标有效DPS
     * 
     * @param dragon 末影龙实体
     * @return 目标DPS (总血量/120秒)
     */
    private static double calculateTargetDPS(EnderDragon dragon) {
        // 末影龙基础生命值为200，但可能被其他模组修改
        float maxHealth = dragon.getMaxHealth();
        // 目标：120秒内击败末影龙
        return maxHealth / 120.0;
    }
    
    /**
     * 计算动态减伤率
     * 公式：y = min(0.80, 1-R/x) 当x>R时，否则为0
     *
     * @param currentDPS 当前DPS
     * @param targetDPS 目标DPS
     * @return 减伤率 (0-0.80)
     */
    private static double calculateDamageReductionInternal(double currentDPS, double targetDPS) {
        if (currentDPS <= targetDPS) {
            return 0.0; // 不超过目标DPS时不减伤
        }

        // 计算减伤率：1 - R/x
        double reduction = 1.0 - (targetDPS / currentDPS);

        // 限制最大减伤率为80%
        return Math.min(reduction, MAX_DAMAGE_REDUCTION);
    }
    
    /**
     * 应用防秒杀机制
     * 
     * @param damage 伤害值
     * @param dragon 末影龙实体
     * @return 应用防秒杀后的伤害值
     */
    private static float applyAntiOHK(float damage, EnderDragon dragon) {
        float maxHealth = dragon.getMaxHealth();
        float maxAllowedDamage = maxHealth * (float) ANTI_OHK_THRESHOLD;
        
        // 限制单次伤害不超过最大生命值的30%
        return Math.min(damage, maxAllowedDamage);
    }
    
    /**
     * 伤害记录数据类
     */
    private static class DamageRecord {
        final float damage;
        final long timestamp;
        
        DamageRecord(float damage, long timestamp) {
            this.damage = damage;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 服务器tick事件监听器，用于定期清理过期数据
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupExpiredData(event.getServer().getTickCount());
            cleanupCounter = 0;
        }
    }
    
    /**
     * 处理末影龙生命值恢复逻辑
     * 当生命值低于30%时，重新放置记录的水晶并恢复生命值
     * 
     * @param dragon 末影龙实体
     */
    private static void processHealthRestoration(EnderDragon dragon) {
        // 检查配置是否启用水晶重生机制
        if (!EverlaArtifactsConfig.isEnderDragonCrystalRespawnEnabled()) {
            return;
        }
        float currentHealth = dragon.getHealth();
        float maxHealth = dragon.getMaxHealth();
        double healthPercentage = currentHealth / maxHealth;
        
        // 检查是否低于30%生命值阈值
        if (healthPercentage >= HEALTH_RESTORE_THRESHOLD) {
            return;
        }
        
        // 检查是否已经执行过恢复
        if (hasRestorationOccurred(dragon)) {
            return;
        }
        
        // 获取记录的水晶位置
        List<BlockPos> storedPositions = getStoredCrystalPositions(dragon);
        
        // 必须要有记录的水晶位置才能执行恢复
        if (storedPositions.isEmpty()) {
            System.out.println("[LunaticDragonBossHandler] 警告：没有记录的水晶位置，无法执行恢复");
            return;
        }
        
        // 执行恢复：重新放置水晶并恢复生命值
        respawnCrystals(dragon, storedPositions);
        dragon.setHealth(maxHealth);
        setRestorationOccurred(dragon, true);
        clearStoredCrystalPositions(dragon);
    }
    
    /**
     * 记录末地主岛范围内有底座的末地水晶位置
     * 
     * @param dragon 末影龙实体
     * @return 水晶位置列表
     */
    private static List<BlockPos> recordCrystalPositions(EnderDragon dragon) {
        List<BlockPos> positions = new ArrayList<>();
        
        // 创建搜索区域（主岛范围内）
        AABB searchArea = new AABB(
            END_PORTAL_CENTER.getX() - MAIN_ISLAND_RADIUS,
            dragon.level().getMinBuildHeight(),
            END_PORTAL_CENTER.getZ() - MAIN_ISLAND_RADIUS,
            END_PORTAL_CENTER.getX() + MAIN_ISLAND_RADIUS,
            dragon.level().getMaxBuildHeight(),
            END_PORTAL_CENTER.getZ() + MAIN_ISLAND_RADIUS
        );
        
        // 查找所有末地水晶（不仅仅是显示底部的）
        List<EndCrystal> allCrystals = dragon.level().getEntitiesOfClass(
            EndCrystal.class, searchArea);
        
        // 筛选有底座的水晶
        for (EndCrystal crystal : allCrystals) {
            if (crystal.showsBottom()) {
                BlockPos pos = crystal.blockPosition();
                positions.add(pos);
            } else {
                // 跳过无底座水晶
            }
        }
        return positions;
    }
    
    /**
     * 重新放置末地水晶
     * 
     * @param dragon 末影龙实体
     * @param positions 水晶位置列表
     */
    private static void respawnCrystals(EnderDragon dragon, List<BlockPos> positions) {
        if (positions.isEmpty()) {
            System.out.println("[LunaticDragonBossHandler] 警告：没有记录的水晶位置");
            return;
        }
        
        // 开始重生末地水晶
        
        int spawnedCount = 0;
        for (BlockPos pos : positions) {
            // 尝试清理位置并放置水晶
            try {
                // 清理当前位置的方块
                dragon.level().removeBlock(pos, false);
                
                // 创建末地水晶
                EndCrystal crystal = new EndCrystal(
                    dragon.level(), 
                    pos.getX() + 0.5, 
                    pos.getY(), 
                    pos.getZ() + 0.5
                );
                crystal.setBeamTarget(END_PORTAL_CENTER);
                
                // 尝试生成水晶
                if (dragon.level().addFreshEntity(crystal)) {
                    spawnedCount++; // 成功重生水晶
                } else {
                    System.out.println("[LunaticDragonBossHandler] 失败：无法在位置 " + pos + " 生成水晶");
                }
            } catch (Exception e) {
                System.out.println("[LunaticDragonBossHandler] 错误：在位置 " + pos + " 重生水晶时发生异常: " + e.getMessage());
            }
        }
        // 水晶重生完成
        
        // 将末影龙瞬移到祭坛顶部
        teleportDragonToPortalTop(dragon);
    }
    
    /**
     * 将末影龙瞬移到末地传送门顶部
     * 
     * @param dragon 末影龙实体
     */
    private static void teleportDragonToPortalTop(EnderDragon dragon) {
        try {
            // 计算传送门顶部位置（通常在Y=70左右）
            BlockPos portalTop = new BlockPos(
                END_PORTAL_CENTER.getX(),
                70, // 传送门顶部Y坐标
                END_PORTAL_CENTER.getZ()
            );
            
            // 设置末影龙位置
            dragon.moveTo(
                portalTop.getX() + 0.5,
                portalTop.getY() + 32.0,
                portalTop.getZ() + 0.5,
                dragon.getYRot(),
                dragon.getXRot()
            );
            
            // 标记位置已更新
            dragon.hurtMarked = true;
        } catch (Exception e) {
            System.out.println("[LunaticDragonBossHandler] 错误：瞬移末影龙时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 从NBT数据获取记录的水晶位置
     * 
     * @param dragon 末影龙实体
     * @return 水晶位置列表
     */
    private static List<BlockPos> getStoredCrystalPositions(EnderDragon dragon) {
        List<BlockPos> positions = new ArrayList<>();
        CompoundTag persistentData = dragon.getPersistentData();
        
        if (persistentData.contains(CRYSTAL_POSITIONS_KEY, ListTag.TAG_LIST)) {
            ListTag positionsTag = persistentData.getList(CRYSTAL_POSITIONS_KEY, ListTag.TAG_INT_ARRAY);
            for (int i = 0; i < positionsTag.size(); i++) {
                int[] coords = positionsTag.getIntArray(i);
                if (coords.length == 3) {
                    positions.add(new BlockPos(coords[0], coords[1], coords[2]));
                }
            }
        }
        
        return positions;
    }
    
    /**
     * 将水晶位置存储到NBT数据
     * 
     * @param dragon 末影龙实体
     * @param positions 水晶位置列表
     */
    private static void storeCrystalPositions(EnderDragon dragon, List<BlockPos> positions) {
        CompoundTag persistentData = dragon.getPersistentData();
        ListTag positionsTag = new ListTag();
        
        for (BlockPos pos : positions) {
            int[] coords = {pos.getX(), pos.getY(), pos.getZ()};
            positionsTag.add(new IntArrayTag(coords));
        }
        
        persistentData.put(CRYSTAL_POSITIONS_KEY, positionsTag);
    }
    
    /**
     * 清理存储的水晶位置数据
     * 
     * @param dragon 末影龙实体
     */
    private static void clearStoredCrystalPositions(EnderDragon dragon) {
        CompoundTag persistentData = dragon.getPersistentData();
        persistentData.remove(CRYSTAL_POSITIONS_KEY);
    }
    
    /**
     * 检查是否已经执行过生命值恢复
     * 
     * @param dragon 末影龙实体
     * @return 是否已执行过恢复
     */
    private static boolean hasRestorationOccurred(EnderDragon dragon) {
        CompoundTag persistentData = dragon.getPersistentData();
        return persistentData.getBoolean(HAS_RESTORATION_OCCURRED_KEY);
    }
    
    /**
     * 设置生命值恢复执行状态
     * 
     * @param dragon 末影龙实体
     * @param occurred 是否已执行
     */
    private static void setRestorationOccurred(EnderDragon dragon, boolean occurred) {
        CompoundTag persistentData = dragon.getPersistentData();
        persistentData.putBoolean(HAS_RESTORATION_OCCURRED_KEY, occurred);
    }
    
    /**
     * 监听实体加入世界事件，在末影龙生成时记录水晶位置
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && !event.getLevel().isClientSide()) {
            // 检查是否启用了月狂或额外模式
            if (dragon.level().getDifficulty() != Difficulty.HARD) {
                return;
            }
            
            GameRules gameRules = dragon.level().getGameRules();
            if (!gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE)) {
                return;
            }
            
            // 延迟执行，确保世界完全加载
            dragon.level().getServer().execute(() -> {
                recordInitialCrystalPositions(dragon);
            });
        }
    }
    
    /**
     * 记录初始水晶位置
     * 
     * @param dragon 末影龙实体
     */
    private static void recordInitialCrystalPositions(EnderDragon dragon) {
        // 检查配置是否启用水晶重生机制
        if (!EverlaArtifactsConfig.isEnderDragonCrystalRespawnEnabled()) {
            return;
        }
        
        // 检查是否已经记录过水晶位置
        List<BlockPos> existingPositions = getStoredCrystalPositions(dragon);
        if (!existingPositions.isEmpty()) {
            return;
        }
        
        // 记录当前有底座的水晶位置
        List<BlockPos> positions = recordCrystalPositions(dragon);
        if (!positions.isEmpty()) {
            storeCrystalPositions(dragon, positions);
        } else {
            System.out.println("[LunaticDragonBossHandler] 警告：末影龙生成时未找到有底座的水晶");
        }
    }
    
    /**
     * 监听末影龙死亡事件，清理相关数据并在月狂或额外模式下生成龙魂碎片
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon) {
            // 清理所有持久化数据
            CompoundTag persistentData = dragon.getPersistentData();
            persistentData.remove(CRYSTAL_POSITIONS_KEY);
            persistentData.remove(HAS_RESTORATION_OCCURRED_KEY);
            
            // 检查是否为月狂或额外模式下的末影龙死亡
            if (isLunaticModeDragon(dragon)) {
                spawnDragonSoulFragments(dragon);
            }
        }
    }
    
    /**
     * 检查是否为月狂或额外模式下的末影龙
     * 
     * @param dragon 末影龙实体
     * @return 是否为月狂或额外模式
     */
    private static boolean isLunaticModeDragon(EnderDragon dragon) {
        return dragon.level().getDifficulty() == Difficulty.HARD && 
               dragon.level().getGameRules().getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE);
    }
    
    /**
     * 在末影龙时生成龙魂碎片
     * 
     * @param dragon 死亡的末影龙实体
     */
    private static void spawnDragonSoulFragments(EnderDragon dragon) {
        try {
            // 计算生成位置（祭坛上方Y=72）
            BlockPos spawnPos = new BlockPos(
                END_PORTAL_CENTER.getX(),
                72,
                END_PORTAL_CENTER.getZ()
            );
            
            // 生成8-12个龙魂碎片
            int fragmentCount = 8 + dragon.level().random.nextInt(5); // 8-12之间随机
            for (int i = 0; i < fragmentCount; i++) {
                // 创建物品栈
                ItemStack fragmentStack = new ItemStack(EverlaartifactsModItems.DRAGON_SOUL_FRAGMENT.get(), 1);
                
                // 创建物品实体
                ItemEntity itemEntity = new ItemEntity(
                    dragon.level(),
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    fragmentStack
                );
                
                // 设置物品实体为失重悬浮状态（无动量）
                itemEntity.setDeltaMovement(0, 0, 0);
                itemEntity.setNoGravity(true);
                
                // 设置无限拾取延迟
                itemEntity.setUnlimitedLifetime();
                
                // 设置发光效果
                itemEntity.setGlowingTag(true);
                
                // 生成物品实体
                dragon.level().addFreshEntity(itemEntity);
            }
        } catch (Exception e) {
            System.out.println("[LunaticDragonBossHandler] 错误：生成龙魂碎片时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期的伤害记录和时间数据（按实体隔离）
     * 
     * @param currentTime 当前服务器tick时间
     */
    private static void cleanupExpiredData(long currentTime) {
        // 清理长时间未活动的实体-玩家数据
        entityPlayerLastDamageTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > DPS_CALCULATION_WINDOW * 2);
        
        // 清理对应的伤害记录
        entityPlayerDamageHistory.entrySet().removeIf(entry -> 
            !entityPlayerLastDamageTime.containsKey(entry.getKey()));
        
        // 清理空的伤害记录列表
        entityPlayerDamageHistory.values().removeIf(List::isEmpty);
    }
    
    /**
     * 测试用方法：清理所有测试数据
     * 注意：仅用于测试环境
     */
    public static void clearTestData() {
        entityPlayerDamageHistory.clear();
        entityPlayerLastDamageTime.clear();
        cleanupCounter = 0;
    }
    

}