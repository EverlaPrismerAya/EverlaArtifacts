package net.everla.everlaartifacts.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EverlaArtifactsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FULL_PROTECTION_MOD_IDS = BUILDER
            .comment("将启用次数盾附魔完整保护功能的模组列表。默认为无尽贪婪相关。")
            .defineListAllowEmpty("fullProtectionModIds", Arrays.asList("avaritia", "re-avaritia", "avaritia_reforged"), o -> o instanceof String);

    private static final ForgeConfigSpec.BooleanValue FORCE_ENABLE_LAYERED_BUFFER = BUILDER
            .comment("是否强制启用次数盾附魔的完整保护功能，无视模组依赖检测。")
            .define("forceEnableLayeredBuffer", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static List<String> getFullProtectionModIds() {
        return FULL_PROTECTION_MOD_IDS.get().stream().map(Object::toString).collect(Collectors.toList());
    }

    public static boolean isFullProtectionModLoaded(String modId) {
        return getFullProtectionModIds().contains(modId);
    }

    public static boolean isForceEnableLayeredBuffer() {
        return FORCE_ENABLE_LAYERED_BUFFER.get();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}