package net.everla.everlaartifacts.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.stream.Collectors;

public class EverlaArtifactsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // 强制启用LayeredBuffer附魔完整保护功能
    public static ForgeConfigSpec.BooleanValue forceEnableLayeredBuffer;
    
    // 配置需要加载的模组ID列表，用于完整保护功能
    public static ForgeConfigSpec.ConfigValue<String> fullProtectionModIds;
    
    // 性能调试模式：当启用时，使用自定义值代替真实硬件信息
    public static ForgeConfigSpec.BooleanValue performanceDebugMode;
    
    // 自定调试CPU数量
    public static ForgeConfigSpec.IntValue customDebugCPUCount;
    
    // 自定义调试内存大小
    public static ForgeConfigSpec.IntValue customDebugMemorySize;
    
    // 穿刺附魔加强配置
    public static ForgeConfigSpec.BooleanValue enhanceImpaling;

    // 红包掉落配置
    public static ForgeConfigSpec.DoubleValue redPacketDropChanceNewYear;
    public static ForgeConfigSpec.DoubleValue redPacketDropChanceChristmas;
    public static ForgeConfigSpec.IntValue redPacketChristmasStartDate;
    public static ForgeConfigSpec.IntValue redPacketChristmasEndDate;
    
    // 末影龙水晶重生机制配置
    public static ForgeConfigSpec.BooleanValue enableEnderDragonCrystalRespawn;
    
    // 禁用真伤的Boss实体ID列表配置
    public static ForgeConfigSpec.ConfigValue<String> disabledTrueDamageBosses;
    
    // 不会被末影人协同攻击的实体ID列表配置
    public static ForgeConfigSpec.ConfigValue<String> immuneToEndermanAggression;
    
    // 凋灵特殊攻击配置
    public static ForgeConfigSpec.BooleanValue enableWitherSpecialAttacks;
    public static ForgeConfigSpec.BooleanValue enableWitherSkeletonSummoning;
    public static ForgeConfigSpec.DoubleValue witherSkeletonSummonHealthThreshold;

    static {
        // 配置穿刺附魔加强
        BUILDER.push("EnhanceImpaling");
        enhanceImpaling = BUILDER.comment("启用穿刺附魔加强：修改伤害公式（等级1+2，后续每级+2.5），并将增伤条件改为任何在雨中、水中或熔岩中的生物").define("enhanceImpaling", true);
        BUILDER.pop();

        // 配置凋灵特殊攻击部分
        BUILDER.push("WitherSpecialAttacks");
        enableWitherSpecialAttacks = BUILDER.comment("启用月狂模式下的凋灵特殊攻击机制").define("enableWitherSpecialAttacks", true);
        enableWitherSkeletonSummoning = BUILDER.comment("启用月狂模式下凋灵生命值低于阈值时召唤特殊凋灵骷髅").define("enableWitherSkeletonSummoning", true);
        witherSkeletonSummonHealthThreshold = BUILDER.comment("凋灵召唤特殊凋灵骷髅的生命值阈值 (0.0-1.0, 例如 0.5 = 50%)").defineInRange("witherSkeletonSummonHealthThreshold", 0.5, 0.0, 1.0);
        BUILDER.pop();

        // 配置末影龙水晶重生机制部分
        BUILDER.push("EnderDragonCrystalRespawn");
        enableEnderDragonCrystalRespawn = BUILDER.comment("启用月狂模式下的末影龙水晶重生机制：当生命值低于30%时重生末地水晶并恢复全部生命值").define("enableEnderDragonCrystalRespawn", true);
        BUILDER.pop();

        // 配置禁用真伤的Boss列表部分
        BUILDER.push("DisabledTrueDamageBosses");
        disabledTrueDamageBosses = BUILDER.comment("月狂模式下禁用真伤效果的Boss实体ID列表，用逗号分隔").define("disabledTrueDamageBosses", "draconicevolution:draconic_guardian,goety:apostle,goety_revelation:summon_apollyon,goety_revelation:apostle_servant");
        BUILDER.pop();
        
        // 配置不会被末影人协同攻击的实体列表部分
        BUILDER.push("ImmuneToEndermanAggression");
        immuneToEndermanAggression = BUILDER.comment("月狂模式下不会被末影人协同攻击的实体ID列表，用逗号分隔").define("immuneToEndermanAggression", "minecraft:enderman,minecraft:shulker,minecraft:endermite,minecraft:ender_dragon");
        BUILDER.pop();

        // 配置红包掉落部分
        BUILDER.push("RedPacketDrop");
        redPacketDropChanceNewYear = BUILDER.comment("新年期间红包掉落概率 (0.0-1.0, 例如 0.01 = 1%)").defineInRange("dropChanceNewYear", 0.01, 0.0, 1.0);
        redPacketDropChanceChristmas = BUILDER.comment("圣诞节期间红包掉落概率 (0.0-1.0, 例如 0.01 = 1%)").defineInRange("dropChanceChristmas", 0.01, 0.0, 1.0);
        redPacketChristmasStartDate = BUILDER.comment("圣诞节开始日期 (月份*100+日期, 例如 1224 = 12月24日)").defineInRange("christmasStartDate", 1224, 101, 1231);
        redPacketChristmasEndDate = BUILDER.comment("圣诞节结束日期 (月份*100+日期, 例如 1231 = 12月31日)").defineInRange("christmasEndDate", 1231, 101, 1231);
        BUILDER.pop();

        // 配置ForceEnableLayeredBuffer部分
        BUILDER.push("ForceEnableLayeredBuffer");
        forceEnableLayeredBuffer = BUILDER.comment("强制启用次数盾（Layered Buffer）附魔的完整保护功能 警告：可能导致轻微性能损失").define("forceEnableLayeredBuffer", false);
        BUILDER.pop();

        // 配置FullProtectionModIds部分
        BUILDER.push("FullProtectionModIds");
        fullProtectionModIds = BUILDER.comment("配置触发次数盾（Layered Buffer）附魔完整保护功能的模组ID列表").define("fullProtectionModIds", "avaritia,re-avaritia,avaritia-reforged,draconicevolution");
        BUILDER.pop();
        
        // 配置PerformanceDebugMode部分
        BUILDER.push("PerformanceDebugMode");
        performanceDebugMode = BUILDER.comment("性能调试模式：当启用时，使用自定义值代替真实硬件信息。警告：游戏默认开启安全验证阻止此自定义，须通过/gamerule ForceUseTruePerformance false禁用").define("performanceDebugMode", false);
        customDebugCPUCount = BUILDER.comment("自定义调试CPU数量：当性能调试模式启用时使用的CPU核心数").defineInRange("customDebugCPUCount", 8, 1, 512);
        customDebugMemorySize = BUILDER.comment("自定义调试内存大小：当性能调试模式启用时使用的内存大小(MB)").defineInRange("customDebugMemorySize", 8192, 1024, 2147483647);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    public static boolean isEnhanceImpaling() {
        return enhanceImpaling.get();
    }

    public static boolean isForceEnableLayeredBuffer() {
        return forceEnableLayeredBuffer.get();
    }
    
    public static String getFullProtectionModIds() {
        return fullProtectionModIds.get();
    }
    
    public static boolean isPerformanceDebugMode() {
        return performanceDebugMode.get();
    }
    
    public static int getCustomDebugCPUCount() {
        return customDebugCPUCount.get();
    }
    
    public static int getCustomDebugMemorySize() {
        return customDebugMemorySize.get();
    }
    
    // 红包掉落配置的获取方法
    public static double getRedPacketDropChanceNewYear() {
        return redPacketDropChanceNewYear.get();
    }
    
    public static double getRedPacketDropChanceChristmas() {
        return redPacketDropChanceChristmas.get();
    }
    
    public static int getRedPacketChristmasStartDate() {
        return redPacketChristmasStartDate.get();
    }
    
    public static int getRedPacketChristmasEndDate() {
        return redPacketChristmasEndDate.get();
    }
    
    // 末影龙水晶重生机制配置的获取方法
    public static boolean isEnderDragonCrystalRespawnEnabled() {
        return enableEnderDragonCrystalRespawn.get();
    }
    
    // 禁用真伤Boss列表配置的获取方法
    public static String getDisabledTrueDamageBosses() {
        return disabledTrueDamageBosses.get();
    }
    
    // 获取解析后的禁用真伤Boss集合
    public static java.util.Set<String> getDisabledTrueDamageBossesSet() {
        String bossesString = disabledTrueDamageBosses.get();
        if (bossesString == null || bossesString.trim().isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return java.util.Arrays.stream(bossesString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }
    
    // 不会被末影人协同攻击的实体配置的获取方法
    public static String getImmuneToEndermanAggression() {
        return immuneToEndermanAggression.get();
    }
    
    // 获取解析后不会被末影人协同攻击的实体集合
    public static java.util.Set<String> getImmuneToEndermanAggressionSet() {
        String entitiesString = immuneToEndermanAggression.get();
        if (entitiesString == null || entitiesString.trim().isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return java.util.Arrays.stream(entitiesString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }
    
    // 凋灵特殊攻击配置的获取方法
    public static boolean isWitherSpecialAttacksEnabled() {
        return enableWitherSpecialAttacks.get();
    }
    
    public static boolean isWitherSkeletonSummoningEnabled() {
        return enableWitherSkeletonSummoning.get();
    }
    
    public static double getWitherSkeletonSummonHealthThreshold() {
        return witherSkeletonSummonHealthThreshold.get();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}