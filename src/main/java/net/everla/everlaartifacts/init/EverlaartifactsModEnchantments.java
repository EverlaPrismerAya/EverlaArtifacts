package net.everla.everlaartifacts.init;

import net.everla.everlaartifacts.common.enchantment.*;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.enchantment.Enchantment;

import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModEnchantments {
	public static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EverlaartifactsMod.MODID);
	public static final RegistryObject<Enchantment> STEADFAST = REGISTRY.register("steadfast", () -> new SteadfastEnchantment());
	public static final RegistryObject<Enchantment> TP_AURA = REGISTRY.register("tp_aura", () -> new TPAuraEnchantment());
	public static final RegistryObject<Enchantment> DEUTSCH = REGISTRY.register("deutsch", () -> new DeutschEnchantment());
	public static final RegistryObject<Enchantment> LAYERED_BUFFER = REGISTRY.register("layered_buffer", () -> new LayeredBufferEnchantment());
	public static final RegistryObject<Enchantment> SCRAPYARD_SCROUNGER = REGISTRY.register("scrapyard_scrounger", () -> new ScrapyardScroungerEnchantment());
	public static final RegistryObject<Enchantment> MONEY_BURNERS_CREED = REGISTRY.register("money_burners_creed", () -> new MoneyBurnersCreedEnchantment());
	public static final RegistryObject<Enchantment> LIVE_WIRE = REGISTRY.register("live_wire", () -> new LiveWireEnchantment());
	public static final RegistryObject<Enchantment> WILD_HUNT = REGISTRY.register("wild_hunt", () -> new WildHuntEnchantment());
	public static final RegistryObject<Enchantment> DEATH_SPRINT = REGISTRY.register("death_sprint", () -> new DeathSprintEnchantment());
	public static final RegistryObject<Enchantment> CHINESE_CAN_FLY = REGISTRY.register("chinese_can_fly", () -> new ChineseCanFlyEnchantment());
	public static final RegistryObject<Enchantment> NVIDIA_NETWORK_QUALITY = REGISTRY.register("nvidia_network_quality", () -> new NvidiaNetworkQualityEnchantment());
	public static final RegistryObject<Enchantment> AMD_SOUND_QUALITY = REGISTRY.register("amd_sound_quality", () -> new AmdSoundQualityEnchantment());
	public static final RegistryObject<Enchantment> WIND_BREATH = REGISTRY.register("wind_breath", () -> new WindBreathEnchantment());
	public static final RegistryObject<Enchantment> ENEMY_STEP = REGISTRY.register("enemy_step", () -> new EnemyStepEnchantment());
}
