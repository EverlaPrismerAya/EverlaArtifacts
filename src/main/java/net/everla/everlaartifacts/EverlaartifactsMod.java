package net.everla.everlaartifacts;

import net.everla.everlaartifacts.server.handlers.difficulty.DifficultySyncHandler;
import net.everla.everlaartifacts.server.network.BloodBlossomEntityPacket;
import net.everla.everlaartifacts.server.network.ClientHardwareInfoPacket;
import net.everla.everlaartifacts.server.network.ClientModCountPacket;
import net.everla.everlaartifacts.server.network.ClientPerformanceReportPacket;
import net.everla.everlaartifacts.server.network.ClientPerformanceStatusPacket;
import net.everla.everlaartifacts.server.network.DifficultyChangePacket;
import net.everla.everlaartifacts.server.network.DifficultySyncPacket;
import net.everla.everlaartifacts.server.network.LanguageSyncPacket;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

import net.everla.everlaartifacts.server.handlers.items.red_packet.RedPacketHandler;
import net.everla.everlaartifacts.server.network.ServerPerformanceScorePacket;
import net.everla.everlaartifacts.init.EverlaartifactsModTabs;
import net.everla.everlaartifacts.init.EverlaartifactsModSounds;
import net.everla.everlaartifacts.init.EverlaartifactsModPotions;
import net.everla.everlaartifacts.init.EverlaartifactsModParticleTypes;
import net.everla.everlaartifacts.init.EverlaartifactsModPaintings;
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.init.EverlaartifactsModFluids;
import net.everla.everlaartifacts.init.EverlaartifactsModFluidTypes;
import net.everla.everlaartifacts.init.EverlaartifactsModTriggerTypes;
import net.everla.everlaartifacts.init.EverlaartifactsModEntities;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.init.EverlaartifactsModLootModifiers;
import net.everla.everlaartifacts.init.EverlaartifactsModBlocks;
import net.everla.everlaartifacts.common.handlers.enchantment.PerformanceBasedThingsHandler;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.everla.everlaartifacts.common.game_rules.ForceUseTruePerformance;
import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;

import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.AbstractMap;

@Mod("everlaartifacts")
public class EverlaartifactsMod {
	public static final Logger LOGGER = LogManager.getLogger(EverlaartifactsMod.class);
	public static final String MODID = "everlaartifacts";

	public EverlaartifactsMod() {
		MinecraftForge.EVENT_BUS.register(RedPacketHandler.class);
		MinecraftForge.EVENT_BUS.register(this);
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener(this::commonSetup);
		EverlaartifactsModSounds.REGISTRY.register(bus);
		EverlaartifactsModBlocks.REGISTRY.register(bus);
		EverlaartifactsModItems.REGISTRY.register(bus);
		EverlaartifactsModEntities.REGISTRY.register(bus);
		EverlaartifactsModEnchantments.REGISTRY.register(bus);
		EverlaartifactsModLootModifiers.REGISTRY.register(bus);
		EverlaartifactsModTabs.REGISTRY.register(bus);
		EverlaartifactsModMobEffects.REGISTRY.register(bus);
		EverlaartifactsModPotions.POTIONS.register(bus);
		EverlaartifactsModPaintings.REGISTRY.register(bus);
		EverlaartifactsModParticleTypes.REGISTRY.register(bus);
		EverlaartifactsModFluids.REGISTRY.register(bus);
		EverlaartifactsModFluidTypes.REGISTRY.register(bus);
		EverlaartifactsModTriggerTypes.register();
		EverlaArtifactsConfig.register();
		initializeSystemInfo();
		registerNetworkPackets();
		// 确保游戏规则类被加载以触发注册
		LOGGER.info("游戏规则 ForceUseTruePerformance 类加载: {}", ForceUseTruePerformance.FORCE_USE_TRUE_PERFORMANCE.toString());
		LOGGER.info("游戏规则 EnableLunaticMode 类加载: {}", EnableLunaticMode.ENABLE_LUNATIC_MODE.toString());
	}
	public void commonSetup(final FMLCommonSetupEvent event){
		EverlaartifactsModPotions.init();
	}
	public static int CPUCoreCount = 0;
	public static int AllocatedRam = 0; // 单位 MB
	
	// 客户端难度状态
	private static String clientDifficultyName = "NORMAL";
	private static boolean clientIsLunaticMode = false;

	/**
	* 初始化系统信息，获取CPU核心数和内存分配大小
	*/
	private void initializeSystemInfo() {
		// 获取CPU核心数
		CPUCoreCount = Runtime.getRuntime().availableProcessors();
		// 获取分配的内存大小（单位MB）
		long maxMemory = Runtime.getRuntime().maxMemory(); // 总分配内存
		AllocatedRam = (int) (maxMemory / (1024 * 1024)); // 转换为MB
		LOGGER.info("系统信息初始化完成 - CPU核心数: {} 核, 分配内存: {} MB", CPUCoreCount, AllocatedRam);
	}
	
	/**
	* 获取客户端当前难度名称
	*/
	public static String getClientDifficultyName() {
		return clientDifficultyName;
	}
	
	/**
	* 获取客户端是否处于月狂模式
	*/
	public static boolean isClientLunaticMode() {
		return clientIsLunaticMode;
	}
	
	/**
	* 设置客户端难度状态
	*/
	public static void setClientDifficulty(String difficultyName, boolean isLunaticMode) {
		clientDifficultyName = difficultyName;
		clientIsLunaticMode = isLunaticMode;
	}
	
	/**
	* 注册网络包
	*/
	private void registerNetworkPackets() {
		addNetworkMessage(ClientPerformanceReportPacket.class, ClientPerformanceReportPacket::encode, ClientPerformanceReportPacket::new,
				ClientPerformanceReportPacket::handle);
		// 添加客户端硬件信息网络包（客户端→服务端，玩家进入游戏时上报物理内存与显存容量）
		addNetworkMessage(ClientHardwareInfoPacket.class, ClientHardwareInfoPacket::encode, ClientHardwareInfoPacket::new,
				ClientHardwareInfoPacket::handle);
		addNetworkMessage(ServerPerformanceScorePacket.class, ServerPerformanceScorePacket::encode, ServerPerformanceScorePacket::new,
				ServerPerformanceScorePacket::handle);
		// 添加BloodBlossomEntityPacket
		addNetworkMessage(BloodBlossomEntityPacket.class, BloodBlossomEntityPacket::encode, BloodBlossomEntityPacket::new,
				BloodBlossomEntityPacket::handle);
		// 添加难度切换网络包
		addNetworkMessage(DifficultyChangePacket.class, DifficultyChangePacket::encode, DifficultyChangePacket::new,
				DifficultyChangePacket::handle);
		// 添加难度同步网络包
		addNetworkMessage(DifficultySyncPacket.class, DifficultySyncPacket::encode, DifficultySyncPacket::new,
				DifficultySyncPacket::handle);
		// 添加语言同步网络包（客户端→服务端，用于中国人能飞附魔）
		addNetworkMessage(LanguageSyncPacket.class, LanguageSyncPacket::encode, LanguageSyncPacket::new,
				LanguageSyncPacket::handle);
		// 添加模组数上报网络包（客户端→服务端，进入游戏时上报安装模组数，供ATM之戒加成）
		addNetworkMessage(ClientModCountPacket.class, ClientModCountPacket::encode, ClientModCountPacket::new,
				ClientModCountPacket::handle);
		// 添加实时性能上报网络包（客户端→服务端，每40刻上报FPS与CPU利用率，合并减少传输开销）
		addNetworkMessage(ClientPerformanceStatusPacket.class, ClientPerformanceStatusPacket::encode, ClientPerformanceStatusPacket::new,
				ClientPerformanceStatusPacket::handle);
	}

	/**
	* 注册并发送性能评分到客户端
	*/
	public static void sendPerformanceScoreToClient(net.minecraft.server.level.ServerPlayer serverPlayer, double performanceScore) {
		ServerPerformanceScorePacket packet = new ServerPerformanceScorePacket(performanceScore);
		net.minecraftforge.network.PacketDistributor.PacketTarget target = net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer);
		PACKET_HANDLER.send(target, packet);
	}

	/**
	* 当玩家加入世界时，处理性能评分和难度同步
	*/
	@SubscribeEvent
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		// 如果是服务端环境，发送当前玩家的性能评分到客户端
		if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			// 先确保玩家的性能评分已设置
			double playerPerformanceScore = PerformanceBasedThingsHandler.getPlayerPerformanceScore(serverPlayer);
			sendPerformanceScoreToClient(serverPlayer, playerPerformanceScore);
			
			// 使用专门的同步处理器来同步难度状态
			DifficultySyncHandler.syncSinglePlayer(serverPlayer);
		}
	}

	/**
	* 玩家登出时清理服务端遥测数据
	*/
	@SubscribeEvent
	public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() != null) {
			PerformanceMetrics.removePlayerFps(event.getEntity().getUUID());
			PerformanceMetrics.removePlayerModCount(event.getEntity().getUUID());
			PerformanceMetrics.removePlayerCpuLoad(event.getEntity().getUUID());
			PerformanceMetrics.removePlayerWindowSize(event.getEntity().getUUID());
		}
	}

	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
	private static int messageID = 0;

	public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
		PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
		messageID++;
	}

	private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

	public static void queueServerWork(int tick, Runnable action) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
			workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
	}

	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
			workQueue.forEach(work -> {
				work.setValue(work.getValue() - 1);
				if (work.getValue() == 0)
					actions.add(work);
			});
			actions.forEach(e -> e.getKey().run());
			workQueue.removeAll(actions);
		}
	}
}
