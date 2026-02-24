package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.server.handlers.difficulty.WorldSeedChecker;
import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 凋灵Boss处理器
 * 当月狂或额外模式开启时（游戏难度为困难且enableLunaticMode游戏规则为true），
 * 实现与末影龙相同的动态减伤机制。
 * 同时在凋灵血量第一次低于50%时生成3个特殊凋灵骷髅。
 */
@Mod.EventBusSubscriber
public class LunaticWitherBossHandler {
    
    // 动态减伤相关常量
    private static final int DPS_CALCULATION_WINDOW = 1200; // 60秒 = 1200 ticks
    private static final double MAX_DAMAGE_REDUCTION = 0.95; // 最大95%减伤
    private static final double ANTI_OHK_THRESHOLD = 0.3; // 防秒杀阈值：单次伤害不超过最大生命值的30%
    
    // DPS跟踪数据结构 - 使用Boss实体UUID+玩家UUID作为复合键
    private static final Map<String, List<DamageRecord>> entityPlayerDamageHistory = new HashMap<>();
    private static final Map<String, Long> entityPlayerLastDamageTime = new HashMap<>();
    
    // 清理计数器
    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 100; // 每100个tick清理一次
    
    // 特殊攻击检测计数器
    private static int specialAttackCheckCounter = 0;
    private static final int SPECIAL_ATTACK_CHECK_INTERVAL = 5; // 每5个tick检测一次特殊攻击
    
    // 凋灵骷髅生成相关常量
    private static final int WITHER_SKELETON_COUNT = 3; // 生成3个凋灵骷髅
    private static final String HAS_SPAWNED_SKELETONS_KEY = "HasSpawnedWitherSkeletons";
    
    // 凋灵实体缓存，提高遍历效率
    private static final Map<net.minecraft.server.level.ServerLevel, List<WitherBoss>> witherCache = new ConcurrentHashMap<>();
    private static final Map<net.minecraft.server.level.ServerLevel, Long> cacheLastAccessTime = new ConcurrentHashMap<>();
    private static int cacheUpdateCounter = 0;
    private static final int CACHE_UPDATE_INTERVAL = 20; // 每秒更新一次缓存
    private static final long CACHE_CLEANUP_INTERVAL = 600; // 30秒清理一次无效缓存
    
    /**
     * 获取当前世界的凋灵实体列表（带缓存）
     * 
     * @param level 服务器世界
     * @return 凋灵实体列表
     */
    private static List<WitherBoss> getWitherEntities(net.minecraft.server.level.ServerLevel level) {
        // 更新访问时间
        cacheLastAccessTime.put(level, level.getGameTime());
        
        // 每CACHE_UPDATE_INTERVAL tick更新一次缓存
        if (cacheUpdateCounter % CACHE_UPDATE_INTERVAL == 0) {
            List<WitherBoss> withers = new ArrayList<>();
            
            // 只遍历玩家附近128方块范围内的凋灵
            for (net.minecraft.world.entity.player.Player player : level.players()) {
                if (player.isSpectator() || player.isCreative()) continue;
                
                net.minecraft.world.phys.AABB searchArea = player.getBoundingBox().inflate(128.0);
                List<WitherBoss> nearbyWithers = level.getEntitiesOfClass(WitherBoss.class, searchArea);
                
                for (WitherBoss wither : nearbyWithers) {
                    if (!withers.contains(wither)) {
                        withers.add(wither);
                    }
                }
            }
            
            witherCache.put(level, withers);
        }
        
        return witherCache.getOrDefault(level, Collections.emptyList());
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWitherSkeletonAttack(LivingAttackEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查受伤实体是否为凋灵骷髅
        if (!(event.getEntity() instanceof WitherSkeleton skeleton)) {
            return;
        }
        
        // 检查是否启用了月狂或额外模式
        if (!isLunaticModeEnabled(event.getEntity().level())) {
            return;
        }
        
        // 检查是否为特殊凋灵骷髅（通过NBT标签识别）
        if (!isSpecialWitherSkeleton(skeleton)) {
            return;
        }
        
        // 检查攻击者是否为凋灵
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof WitherBoss) {
            // 取消攻击事件，使特殊凋灵骷髅在月狂或额外模式下完全免疫凋灵攻击
            // 这会阻止伤害、击退、闪红等所有攻击效果
            event.setCanceled(true);
            return;
        }
    }
    

    
    /**
     * 检查Extra难度下凋灵受到in_wall伤害的情况
     * 
     * @param wither 凋灵实体
     * @param event 伤害事件
     */
    private static void checkExtraDifficultyInWallDamage(WitherBoss wither, LivingHurtEvent event) {
        // 检查是否为Extra难度
        if (wither.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            boolean isExtraDifficulty = WorldSeedChecker.isSpecialSeedWorld() && 
                WorldSeedChecker.getCurrentWorldDifficulty(serverLevel.getServer()) == DifficultyLevel.EXTRA;
            
            if (isExtraDifficulty) {
                // 检查是否为in_wall伤害
                DamageSource damageSource = event.getSource();
                if (damageSource.is(DamageTypes.IN_WALL)) {
                    // 在服务端播放everlaartifacts:live_wire声音
                    wither.level().playSound(
                        null, // 不指定特定玩家
                        wither.getX(), 
                        wither.getY(), 
                        wither.getZ(),
                        net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("everlaartifacts", "live_wire")
                        ),
                        SoundSource.HOSTILE,
                        5.0F,
                        1.0F
                    );
                }
            }
        }
    }
    
    /**
     * 监听生物受伤事件，处理凋灵的动态减伤和防秒杀逻辑
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查受伤实体是否为凋灵
        if (!(event.getEntity() instanceof WitherBoss wither)) {
            return;
        }
        
        // 检查是否为Extra难度且受到in_wall伤害
        checkExtraDifficultyInWallDamage(wither, event);
        
        // 检查是否启用了月狂或额外模式且世界难度为困难
        if (wither.level().getDifficulty() != Difficulty.HARD) {
            return;
        }
        
        GameRules gameRules = wither.level().getGameRules();
        if (!gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE)) {
            return;
        }
        
        // 处理动态减伤和防秒杀逻辑
        processDynamicDamageReduction(event, wither);
        
        // 处理凋灵骷髅生成逻辑
        processWitherSkeletonSpawn(wither);
        // 特殊攻击逻辑已移到服务器tick事件中每刻触发
    }
    
    /**
     * 监听凋灵死亡事件，清理相关数据
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof WitherBoss wither) {
            // 清理所有持久化数据
            CompoundTag persistentData = wither.getPersistentData();
            persistentData.remove(HAS_SPAWNED_SKELETONS_KEY);
        }
    }
    
    /**
     * 监听实体加入世界事件
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof WitherBoss wither && !event.getLevel().isClientSide()) {
            // 检查是否启用了月狂或额外模式
            if (wither.level().getDifficulty() != Difficulty.HARD) {
                return;
            }
            
            GameRules gameRules = wither.level().getGameRules();
            if (!gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE)) {
                return;
            }
            
            // 初始化数据
            initializeWitherData(wither);
            
            // 记录生成时间并明确禁用特殊攻击
            LunaticWitherSpecialAttack.setSpawnTime(wither, wither.level().getGameTime());
            LunaticWitherSpecialAttack.setSpecialAttackEnabled(wither, false);
            
            // 确保初始化所有相关NBT数据
            initializeWitherSpecialAttackData(wither);
            
            // 初始化状态确认
        }
    }
    
    /**
     * 服务器tick事件监听器，用于定期清理过期数据和检查特殊攻击
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
        
        // 更新缓存计数器
        cacheUpdateCounter++;
        
        // 特殊攻击检测计数器
        specialAttackCheckCounter++;
        if (specialAttackCheckCounter >= SPECIAL_ATTACK_CHECK_INTERVAL) {
            // 每5刻检查所有世界中的凋灵特殊攻击
            checkWitherSpecialAttacks(event.getServer().getAllLevels());
            specialAttackCheckCounter = 0;
        }
        
        // 定期清理无效缓存
        if (cacheUpdateCounter % CACHE_CLEANUP_INTERVAL == 0) {
            cleanupInvalidCache(event.getServer().getAllLevels());
        }
    }
    
    /**
     * 处理动态减伤和防秒杀逻辑
     * 
     * @param event 伤害事件
     * @param wither 凋灵实体
     */
    private static void processDynamicDamageReduction(LivingHurtEvent event, WitherBoss wither) {
        DamageSource damageSource = event.getSource();
        Entity attacker = damageSource.getEntity();
        
        // 只处理玩家造成的伤害
        if (!(attacker instanceof Player player)) {
            return;
        }
        
        UUID playerUUID = player.getUUID();
        UUID entityUUID = wither.getUUID();
        float originalDamage = event.getAmount();
        long currentTime = wither.level().getGameTime();
        
        // 记录伤害历史（按实体隔离）
        recordPlayerDamage(entityUUID, playerUUID, originalDamage, currentTime);
        
        // 计算玩家当前DPS（按实体隔离）
        double currentDPS = calculatePlayerDPS(entityUUID, playerUUID, currentTime);
        
        // 计算目标有效DPS (总血量/120秒)
        double targetDPS = calculateTargetDPS(wither);
        
        // 计算减伤率
        double reductionRate = calculateDamageReductionInternal(currentDPS, targetDPS);
        
        // 应用动态减伤
        float reducedDamage = (float) (originalDamage * (1.0 - reductionRate));
        
        // 应用防秒杀机制
        float antiOHKDamage = applyAntiOHK(reducedDamage, wither);
        
        // 设置最终伤害值
        event.setAmount(antiOHKDamage);
        
        // 更新最后一次伤害时间（按实体隔离）
        entityPlayerLastDamageTime.put(getEntityPlayerKey(entityUUID, playerUUID), currentTime);
    }

    /**
     * 处理凋灵骷髅生成逻辑
     * 
     * @param wither 凋灵实体
     */
    private static void processWitherSkeletonSpawn(WitherBoss wither) {
        // 检查配置是否启用凋灵骷髅召唤
        if (!EverlaArtifactsConfig.isWitherSkeletonSummoningEnabled()) {
            return;
        }
        
        // 检查是否已经生成过凋灵骷髅
        if (hasSpawnedWitherSkeletons(wither)) {
            return;
        }
        
        float currentHealth = wither.getHealth();
        float maxHealth = wither.getMaxHealth();
        double healthPercentage = currentHealth / maxHealth;
        
        // 使用配置的生命值阈值
        double healthThreshold = EverlaArtifactsConfig.getWitherSkeletonSummonHealthThreshold();
        
        // 检查是否低于配置的生命值阈值
        if (healthPercentage >= healthThreshold) {
            return;
        }
        
        // 生成特殊的凋灵骷髅
        spawnSpecialWitherSkeletons(wither);
        
        // 标记已生成
        setHasSpawnedWitherSkeletons(wither, true);
    }
    
    /**
     * 生成特殊的凋灵骷髅
     * 
     * @param wither 凋灵实体
     */
    private static void spawnSpecialWitherSkeletons(WitherBoss wither) {
        try {
            BlockPos witherPos = wither.blockPosition();
            
            for (int i = 0; i < WITHER_SKELETON_COUNT; i++) {
                // 计算生成位置（围绕凋灵随机分布）
                double angle = (2 * Math.PI * i) / WITHER_SKELETON_COUNT;
                double radius = 3.0 + wither.level().random.nextDouble() * 2.0; // 3-5格距离
                
                BlockPos spawnPos = new BlockPos(
                    witherPos.getX() + (int)(Math.cos(angle) * radius),
                    witherPos.getY(),
                    witherPos.getZ() + (int)(Math.sin(angle) * radius)
                );
                
                // 创建凋灵骷髅
                WitherSkeleton skeleton = new WitherSkeleton(net.minecraft.world.entity.EntityType.WITHER_SKELETON, wither.level());
                skeleton.moveTo(spawnPos, 0, 0);
                
                // 设置凋灵骷髅生命值（根据难度动态调整）
                setWitherSkeletonHealth(skeleton, wither.level());
                
                // 装备下界合金套装
                equipNetheriteArmor(skeleton);
                
                // 设置装备掉落概率为0（不会掉落装备）
                setNoEquipmentDrop(skeleton);
                
                // 给予抗性提升V 5秒
                skeleton.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                
                // 使凋灵骷髅免疫凋灵伤害
                makeImmuneToWitherDamage(skeleton);
                
                // 生成凋灵骷髅
                wither.level().addFreshEntity(skeleton);
            }
        } catch (Exception e) {
            System.out.println("[LunaticWitherBossHandler] 错误：生成凋灵骷髅时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 根据游戏难度设置凋灵骷髅生命值
     * 月狂模式：40点血量
     * 额外模式：85点血量
     * 
     * @param skeleton 凋灵骷髅实体
     * @param level 世界对象
     */
    private static void setWitherSkeletonHealth(WitherSkeleton skeleton, net.minecraft.world.level.Level level) {
        double healthValue = 30.0; // 默认血量
        
        // 检查是否为服务器世界
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // 检查是否为特殊种子世界
            if (WorldSeedChecker.isSpecialSeedWorld()) {
                DifficultyLevel difficulty = WorldSeedChecker.getCurrentWorldDifficulty(serverLevel.getServer());
                
                switch (difficulty) {
                    case LUNATIC:
                        healthValue = 30.0;
                        break;
                    case EXTRA:
                        healthValue = 55.0;
                        break;
                    default:
                        healthValue = 30.0;
                        break;
                }
            }
        }
        
        // 设置最大生命值和当前生命值
        skeleton.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(healthValue);
        skeleton.setHealth((float) healthValue);
    }
    
    /**
     * 为凋灵骷髅装备下界合金套装（不会掉落）
     * 
     * @param skeleton 凋灵骷髅实体
     */
    private static void equipNetheriteArmor(WitherSkeleton skeleton) {
        // 下界合金头盔
        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        EnchantmentHelper.setEnchantments(
                Map.of(Enchantments.ALL_DAMAGE_PROTECTION, 8),
                helmet
        );
        // 下界合金胸甲（带狂猎附魔）
        ItemStack chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);
        EnchantmentHelper.setEnchantments(
            Map.of(EverlaartifactsModEnchantments.WILD_HUNT.get(), 1,
                    Enchantments.ALL_DAMAGE_PROTECTION, 8),

            chestplate
        );
        // 下界合金护腿
        ItemStack leggings = new ItemStack(Items.NETHERITE_LEGGINGS);
        EnchantmentHelper.setEnchantments(
                Map.of(Enchantments.ALL_DAMAGE_PROTECTION, 8),
                leggings
        );
        // 下界合金靴子
        ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
        EnchantmentHelper.setEnchantments(
                Map.of(Enchantments.ALL_DAMAGE_PROTECTION, 8),
                boots
        );
        
        // 下界合金剑（锋利V）
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        EnchantmentHelper.setEnchantments(
            Map.of(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 10),
            sword
        );
        
        // 装备护甲
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chestplate);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, leggings);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, boots);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
        
        // 设置护甲为无法破坏
        setUnbreakable(helmet);
        setUnbreakable(chestplate);
        setUnbreakable(leggings);
        setUnbreakable(boots);
        setUnbreakable(sword);
    }
    
    /**
     * 设置实体的装备掉落概率为0
     * 
     * @param skeleton 凋灵骷髅实体
     */
    private static void setNoEquipmentDrop(WitherSkeleton skeleton) {
        // 设置所有装备槽的掉落概率为0
        skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.0f);
        skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.CHEST, 0.0f);
        skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.LEGS, 0.0f);
        skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.FEET, 0.0f);
        skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);
        skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.OFFHAND, 0.0f);
    }
    
    /**
     * 设置物品为无法破坏
     * 
     * @param stack 物品栈
     */
    private static void setUnbreakable(ItemStack stack) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }
        stack.getTag().putBoolean("Unbreakable", true);
    }
    
    /**
     * 使凋灵骷髅免疫凋灵伤害
     * 
     * @param skeleton 凋灵骷髅实体
     */
    private static void makeImmuneToWitherDamage(WitherSkeleton skeleton) {
        // 这里可以通过自定义伤害类型或事件监听来实现免疫
        // 目前通过NBT标签标记
        if (!skeleton.getPersistentData().contains("ImmuneToWitherDamage")) {
            skeleton.getPersistentData().putBoolean("ImmuneToWitherDamage", true);
        }
    }
    
    // 以下方法复制自LunaticDragonBossHandler的动态减伤逻辑
    
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
     * @param wither 凋灵实体
     * @return 目标DPS (总血量/120秒)
     */
    private static double calculateTargetDPS(WitherBoss wither) {
        // 凋灵基础生命值为300，但可能被其他模组修改
        float maxHealth = wither.getMaxHealth();
        // 目标：120秒内击败凋灵
        return maxHealth / 120.0;
    }
    
    /**
     * 计算动态减伤率
     * 公式：y = min(0.95, 1-R/x) 当x>R时，否则为0
     * 
     * @param currentDPS 当前DPS
     * @param targetDPS 目标DPS
     * @return 减伤率 (0-0.95)
     */
    private static double calculateDamageReductionInternal(double currentDPS, double targetDPS) {
        if (currentDPS <= targetDPS) {
            return 0.0; // 不超过目标DPS时不减伤
        }
        
        // 计算减伤率：1 - R/x
        double reduction = 1.0 - (targetDPS / currentDPS);
        
        // 限制最大减伤率为95%
        return Math.min(reduction, MAX_DAMAGE_REDUCTION);
    }
    
    /**
     * 应用防秒杀机制
     * 
     * @param damage 伤害值
     * @param wither 凋灵实体
     * @return 应用防秒杀后的伤害值
     */
    private static float applyAntiOHK(float damage, WitherBoss wither) {
        float maxHealth = wither.getMaxHealth();
        float maxAllowedDamage = maxHealth * (float) ANTI_OHK_THRESHOLD;
        
        // 限制单次伤害不超过最大生命值的30%
        return Math.min(damage, maxAllowedDamage);
    }
    
    /**
     * 检查是否已经生成过凋灵骷髅
     * 
     * @param wither 凋灵实体
     * @return 是否已生成
     */
    private static boolean hasSpawnedWitherSkeletons(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getBoolean(HAS_SPAWNED_SKELETONS_KEY);
    }
    
    /**
     * 设置凋灵骷髅生成状态
     * 
     * @param wither 凋灵实体
     * @param spawned 是否已生成
     */
    private static void setHasSpawnedWitherSkeletons(WitherBoss wither, boolean spawned) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putBoolean(HAS_SPAWNED_SKELETONS_KEY, spawned);
    }
    
    /**
     * 初始化凋灵数据
     * 
     * @param wither 凋灵实体
     */
    private static void initializeWitherData(WitherBoss wither) {
        // 确保NBT数据初始化
        CompoundTag persistentData = wither.getPersistentData();
        if (!persistentData.contains(HAS_SPAWNED_SKELETONS_KEY)) {
            persistentData.putBoolean(HAS_SPAWNED_SKELETONS_KEY, false);
        }
    }
    
    /**
     * 初始化凋灵特殊攻击相关数据
     * 
     * @param wither 凋灵实体
     */
    private static void initializeWitherSpecialAttackData(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        
        // 明确初始化特殊攻击状态为false
        persistentData.putBoolean(LunaticWitherSpecialAttack.SPECIAL_ATTACK_ENABLED_KEY, false);
        
        // 初始化生成时间
        if (!persistentData.contains(LunaticWitherSpecialAttack.SPAWN_TIME_KEY)) {
            persistentData.putLong(LunaticWitherSpecialAttack.SPAWN_TIME_KEY, wither.level().getGameTime());
        }
        
        // 初始化其他相关状态
        if (!persistentData.contains(LunaticWitherSpecialAttack.LAST_CHARGE_TIME_KEY)) {
            persistentData.putLong(LunaticWitherSpecialAttack.LAST_CHARGE_TIME_KEY, 0L);
        }
        if (!persistentData.contains(LunaticWitherSpecialAttack.LAST_SMASH_TIME_KEY)) {
            persistentData.putLong(LunaticWitherSpecialAttack.LAST_SMASH_TIME_KEY, 0L);
        }
        if (!persistentData.contains(LunaticWitherSpecialAttack.IS_CHARGING_KEY)) {
            persistentData.putBoolean(LunaticWitherSpecialAttack.IS_CHARGING_KEY, false);
        }
        if (!persistentData.contains(LunaticWitherSpecialAttack.IS_SMASHING_KEY)) {
            persistentData.putBoolean(LunaticWitherSpecialAttack.IS_SMASHING_KEY, false);
        }
    }

    /**
     * 每刻检查所有世界中的凋灵特殊攻击
     * 使用缓存机制提高遍历效率
     * 改为基于Invul标签判断是否启用特殊攻击
     * 
     * @param levels 所有世界
     */
    private static void checkWitherSpecialAttacks(Iterable<net.minecraft.server.level.ServerLevel> levels) {
        // 检查配置是否启用凋灵特殊攻击
        if (!EverlaArtifactsConfig.isWitherSpecialAttacksEnabled()) {
            return;
        }
        
        for (net.minecraft.server.level.ServerLevel level : levels) {
            if (level.isClientSide()) continue;
            
            // 检查是否启用了月狂或额外模式
            if (!isLunaticModeEnabled(level)) {
                continue;
            }
            
            // 使用缓存获取凋灵实体列表
            List<WitherBoss> withers = getWitherEntities(level);
            
            for (WitherBoss wither : withers) {
                // 检查是否可以启用特殊攻击（基于Invul标签判断）
                if (!LunaticWitherSpecialAttack.canEnableSpecialAttack(wither)) {
                    // 如果不能启用且当前状态为启用，则禁用特殊攻击
                    if (LunaticWitherSpecialAttack.isSpecialAttackEnabled(wither)) {
                        LunaticWitherSpecialAttack.setSpecialAttackEnabled(wither, false);
                    }
                    continue;
                }
                
                // 如果可以启用但当前状态为禁用，则启用特殊攻击
                if (!LunaticWitherSpecialAttack.isSpecialAttackEnabled(wither)) {
                    LunaticWitherSpecialAttack.setSpecialAttackEnabled(wither, true);
                }
                
                // 处理凋灵特殊攻击逻辑（每刻触发）
                LunaticWitherSpecialAttack.processWitherChargeAttack(wither);
                LunaticWitherSpecialAttack.processWitherSmashAttack(wither);
            }
        }
    }
    
    /**
     * 清理无效的凋灵缓存
     * 
     * @param levels 所有世界
     */
    private static void cleanupInvalidCache(Iterable<net.minecraft.server.level.ServerLevel> levels) {
        long currentTime = 0;
        
        // 获取当前时间
        for (net.minecraft.server.level.ServerLevel level : levels) {
            if (!level.isClientSide()) {
                currentTime = level.getGameTime();
                break;
            }
        }
        
        if (currentTime == 0) return;
        
        // 清理长时间未访问的世界缓存
        final long finalCurrentTime = currentTime;
        cacheLastAccessTime.entrySet().removeIf(entry -> {
            net.minecraft.server.level.ServerLevel level = entry.getKey();
            long lastAccessTime = entry.getValue();
            
            // 如果世界已被卸载或长时间未访问，则清理缓存
            if (level.isClientSide() || finalCurrentTime - lastAccessTime > CACHE_CLEANUP_INTERVAL * 2) {
                witherCache.remove(level);
                return true;
            }
            return false;
        });
        
        // 清理已移除的凋灵实体引用
        witherCache.replaceAll((level, withers) -> {
            if (withers != null) {
                withers.removeIf(wither -> wither == null || wither.isRemoved());
            }
            return withers;
        });
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
     * 检查是否为特殊凋灵骷髅
     * 通过检查NBT标签中的特殊标记来识别
     * 
     * @param skeleton 凋灵骷髅实体
     * @return 是否为特殊凋灵骷髅
     */
    private static boolean isSpecialWitherSkeleton(WitherSkeleton skeleton) {
        return skeleton.getPersistentData().getBoolean("ImmuneToWitherDamage");
    }
    
    /**
     * 测试用方法：清理所有测试数据
     * 注意：仅用于测试环境
     */
    public static void clearTestData() {
        entityPlayerDamageHistory.clear();
        entityPlayerLastDamageTime.clear();
        cleanupCounter = 0;
        specialAttackCheckCounter = 0;
        witherCache.clear();
        cacheLastAccessTime.clear();
    }
}
