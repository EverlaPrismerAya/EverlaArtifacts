package net.everla.everlaartifacts.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class EverlaArtifactsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // 强制启用LayeredBuffer附魔完整保护功能
    public static ForgeConfigSpec.BooleanValue forceEnableLayeredBuffer;
    
    // 配置需要加载的模组ID列表，用于完整保护功能
    public static ForgeConfigSpec.ConfigValue<String> fullProtectionModIds;
    
    // 性能调试模式：当启用时，使用自定义值代替真实硬件信息
    public static ForgeConfigSpec.BooleanValue performanceDebugMode;
    
    // 自定义调试CPU数量
    public static ForgeConfigSpec.IntValue customDebugCPUCount;
    
    // 自定义调试内存大小
    public static ForgeConfigSpec.IntValue customDebugMemorySize;

    static {
        // 配置ForceEnableLayeredBuffer部分
        BUILDER.push("ForceEnableLayeredBuffer");
        forceEnableLayeredBuffer = BUILDER.comment("强制启用LayeredBuffer附魔完整保护功能").define("forceEnableLayeredBuffer", false);
        BUILDER.pop();

        // 配置FullProtectionModIds部分
        BUILDER.push("FullProtectionModIds");
        fullProtectionModIds = BUILDER.comment("配置需要加载的模组ID列表，用于完整保护功能").define("fullProtectionModIds", "avaritia,re-avaritia,avaritia-reforged");
        BUILDER.pop();
        
        // 配置PerformanceDebugMode部分
        BUILDER.push("PerformanceDebugMode");
        performanceDebugMode = BUILDER.comment("性能调试模式：当启用时，使用自定义值代替真实硬件信息。警告：游戏默认开启安全验证阻止此自定义，须通过/gamerule ForceUseTruePerformance false禁用").define("performanceDebugMode", false);
        customDebugCPUCount = BUILDER.comment("自定义调试CPU数量：当性能调试模式启用时使用的CPU核心数").defineInRange("customDebugCPUCount", 8, 1, 128);
        customDebugMemorySize = BUILDER.comment("自定义调试内存大小：当性能调试模式启用时使用的内存大小(MB)").defineInRange("customDebugMemorySize", 8192, 512, 65536);
        BUILDER.pop();

        SPEC = BUILDER.build();
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
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}