package net.everla.everlaartifacts;

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

import net.everla.everlaartifacts.network.ServerPerformanceScorePacket;
import net.everla.everlaartifacts.init.EverlaartifactsModTabs;
import net.everla.everlaartifacts.init.EverlaartifactsModSounds;
import net.everla.everlaartifacts.init.EverlaartifactsModPotions;
import net.everla.everlaartifacts.init.EverlaartifactsModPaintings;
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.init.EverlaartifactsModFluids;
import net.everla.everlaartifacts.init.EverlaartifactsModFluidTypes;
import net.everla.everlaartifacts.init.EverlaartifactsModEntities;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.init.EverlaartifactsModBlocks;
import net.everla.everlaartifacts.game_rules.ForceUseTruePerformance;
import net.everla.everlaartifacts.config.EverlaArtifactsConfig;

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
		// Start of user code block mod constructor
		// End of user code block mod constructor
		MinecraftForge.EVENT_BUS.register(this);
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		EverlaartifactsModSounds.REGISTRY.register(bus);
		EverlaartifactsModBlocks.REGISTRY.register(bus);
		EverlaartifactsModItems.REGISTRY.register(bus);
		EverlaartifactsModEntities.REGISTRY.register(bus);
		EverlaartifactsModEnchantments.REGISTRY.register(bus);
		EverlaartifactsModTabs.REGISTRY.register(bus);
		EverlaartifactsModMobEffects.REGISTRY.register(bus);
		EverlaartifactsModPotions.REGISTRY.register(bus);
		EverlaartifactsModPaintings.REGISTRY.register(bus);
		EverlaartifactsModFluids.REGISTRY.register(bus);
		EverlaartifactsModFluidTypes.REGISTRY.register(bus);
		// Start of user code block mod init
		EverlaArtifactsConfig.register();
		initializeSystemInfo();
		registerNetworkPackets();
		// 确保游戏规则类被加载以触发注册
		LOGGER.info("游戏规则 ForceUseTruePerformance 类加载: {}", ForceUseTruePerformance.FORCE_USE_TRUE_PERFORMANCE.toString());
		// End of user code block mod init
	}

	// Start of user code block mod methods
	// 系统信息全局变量
	public static int CPUCoreCount = 0;
	public static int AllocatedRam = 0; // 单位 MB

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
	* 注册网络包
	*/
	private void registerNetworkPackets() {
		addNetworkMessage(net.everla.everlaartifacts.network.ClientPerformanceReportPacket.class, net.everla.everlaartifacts.network.ClientPerformanceReportPacket::encode, net.everla.everlaartifacts.network.ClientPerformanceReportPacket::new,
				net.everla.everlaartifacts.network.ClientPerformanceReportPacket::handle);
		addNetworkMessage(net.everla.everlaartifacts.network.ServerPerformanceScorePacket.class, net.everla.everlaartifacts.network.ServerPerformanceScorePacket::encode, net.everla.everlaartifacts.network.ServerPerformanceScorePacket::new,
				net.everla.everlaartifacts.network.ServerPerformanceScorePacket::handle);
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
	* 当玩家加入世界时，处理性能评分
	*/
	@SubscribeEvent
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		// 在服务端环境，只记录日志
		if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
			// 客户端逻辑已在ClientEventHandler中处理
		} else {
			// 在服务端环境（包括集成服务器），我们不做任何处理
			// 因为性能评分应该通过网络包从客户端接收
			// 日志已被移除
		}
		// 如果是服务端环境，发送当前玩家的性能评分到客户端
		if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			// 先确保玩家的性能评分已设置
			double playerPerformanceScore = PerformanceBasedThingsHandler.getPlayerPerformanceScore(serverPlayer);
			sendPerformanceScoreToClient(serverPlayer, playerPerformanceScore);
		}
	}

	// End of user code block mod methods
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
