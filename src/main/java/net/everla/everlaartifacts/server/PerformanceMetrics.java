package net.everla.everlaartifacts.server;

import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import org.lwjgl.opengl.ATIMeminfo;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.NVXGPUMemoryInfo;
import org.lwjgl.system.MemoryStack;

import java.lang.management.ManagementFactory;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 性能指标计算和网络传输类
 */
public class PerformanceMetrics {
    // 评分函数参数
    private static final int BASE_CPU_CORES = 12;     // 基准CPU核心数
    private static final int BASE_MEMORY_MB = 8192;   // 基准内存大小 (MB)
    private static final int MAX_CPU_CORES = 32;      // 最大CPU核心数
    private static final int MAX_MEMORY_MB = 16384;   // 最大内存大小 (MB)
    private static final int MIN_CPU_CORES = 8;       // 最小CPU核心数
    private static final int MIN_MEMORY_MB = 4096;    // 最小内存大小 (MB)
    private static final int MAX_SCORE = 100;         // 最高分数
    private static final int MIN_SCORE = -100;        // 最低分数
    private static final double CPU_WEIGHT = 0.8;     // CPU权重
    private static final double MEMORY_WEIGHT = 0.2;  // 内存权重
    
    private static final Random random = new Random();
    
    // 存储调试模式下的自定义数
    private static Integer debugCPUCount = null;
    private static Integer debugMemorySize = null;
    
    // 存储从服务端接收的性能评分
    private static Double serverPerformanceScore = null;
    
    // 存储从服务端接收的硬件信息
    private static Integer serverCPUCount = null;
    private static Integer serverAllocatedMemory = null;

    // 存储从客户端接收的硬件信息（性能遥测）
    private static Integer receivedPhysicalMemoryMB = null;
    private static Integer receivedVramMB = null;

    /**
     * CPU评分函数: 根据CPU核心数计算相对于基准的评分
     *
     * @param cpuCore CPU核心数
     * @return CPU评分
     */
    public static double calculateCPUScore(int cpuCore) {
        // 计算相对于基准值的比例
        double cpuRatio = (double)(cpuCore - BASE_CPU_CORES) / (MAX_CPU_CORES - MIN_CPU_CORES);
        
        // 将比例转换为分数范围 [-100, 100]
        double cpuScore = cpuRatio * (MAX_SCORE - MIN_SCORE);
        
        // 限制分数在范围内
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, cpuScore));
    }

    /**
     * 内存评分函数: 根据分配的内存大小计算相对于基准的评分
     *
     * @param allocatedMemory 分配的内存大小 (MB)
     * @return 内存评分
     */
    public static double calculateMemoryScore(int allocatedMemory) {
        // 计算相对于基准值的比例
        double memoryRatio = (double)(allocatedMemory - BASE_MEMORY_MB) / (MAX_MEMORY_MB - MIN_MEMORY_MB);
        
        // 将比例转换为分数范围 [-100, 100]
        double memoryScore = memoryRatio * (MAX_SCORE - MIN_SCORE);
        
        // 限制分数在范围内
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, memoryScore));
    }

    /**
     * 总评分函数: S(c,m) = (wc * F(c) + wm * G(m))
     *
     * @param cpuCore         CPU核心数
     * @param allocatedMemory 分配的内存大小 (MB)
     * @return 总评分
     */
    public static double calculateTotalScore(int cpuCore, int allocatedMemory) {
        double cpuScore = calculateCPUScore(cpuCore);
        double memoryScore = calculateMemoryScore(allocatedMemory);
        return CPU_WEIGHT * cpuScore + MEMORY_WEIGHT * memoryScore;
    }

    /**
     * 获取当前客户端的性能评分
     *
     * @return 性能评分
     */
    public static double getClientPerformanceScore() {
        // 如果从服务端接收到了性能评分，则优先使用服务端的评分
        if (serverPerformanceScore != null) {
            return serverPerformanceScore;
        }
        
        int cpuCores = getClientCPUCount();
        int allocatedMemoryMB = getClientAllocatedMemory();
        
        return calculateTotalScore(cpuCores, allocatedMemoryMB);
    }
    
    /**
     * 设置从服务端接收的性能评分
     *
     * @param score 从服务端接收到的性能评分
     */
    public static void setClientPerformanceScore(double score) {
        serverPerformanceScore = score;
    }
    
    /**
     * 重置从服务端接收的性能评分（例如玩家离开服务器时）
     */
    public static void resetClientPerformanceScore() {
        serverPerformanceScore = null;
    }
    
    /**
     * 设置从服务端接收的硬件信息
     *
     * @param cpuCount 服务器CPU核心数
     * @param allocatedMemory 服务器分配内存大小（MB）
     */
    public static void setServerHardwareInfo(int cpuCount, int allocatedMemory) {
        serverCPUCount = cpuCount;
        serverAllocatedMemory = allocatedMemory;
    }
    
    /**
     * 获取从服务端接收的CPU核心数
     *
     * @return 服务器CPU核心数，如果未接收则返回null
     */
    public static Integer getServerCPUCount() {
        return serverCPUCount;
    }
    
    /**
     * 获取从服务端接收的分配内存大小
     *
     * @return 服务器分配内存大小（MB），如果未接收则返回null
     */
    public static Integer getServerAllocatedMemory() {
        return serverAllocatedMemory;
    }
    
    /**
     * 重置从服务端接收的硬件信息（例如玩家离开服务器时）
     */
    public static void resetServerHardwareInfo() {
        serverCPUCount = null;
        serverAllocatedMemory = null;
    }

    /**
     * 设置从客户端接收的硬件信息（物理内存与显存容量，性能遥测用）
     *
     * @param physicalMemoryMB 客户端设备物理内存总容量（MB）
     * @param vramMB           客户端显卡显存容量（MB）
     */
    public static void receiveClientHardwareInfo(int physicalMemoryMB, int vramMB) {
        receivedPhysicalMemoryMB = physicalMemoryMB;
        receivedVramMB = vramMB;
    }

    /**
     * 获取从客户端接收的物理内存容量（MB）
     *
     * @return 物理内存容量（MB），未接收时返回null
     */
    public static Integer getReceivedPhysicalMemoryMB() {
        return receivedPhysicalMemoryMB;
    }

    /**
     * 获取从客户端接收的显存容量（MB）
     *
     * @return 显存容量（MB），未接收时返回null
     */
    public static Integer getReceivedVramMB() {
        return receivedVramMB;
    }

    /**
     * 检测设备物理内存总容量（MB）
     * <p>
     * 通过 {@code com.sun.management.OperatingSystemMXBean} 获取系统物理内存，
     * 而非 JVM 堆内存上限。失败时按 0 处理。
     *
     * @return 物理内存容量（MB），获取失败时返回0
     */
    public static int detectPhysicalMemoryMB() {
        try {
            long totalBytes = ((com.sun.management.OperatingSystemMXBean)
                    ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
            return (int) (totalBytes / (1024 * 1024));
        } catch (Throwable e) {
            // 无法获取物理内存时按 0 处理
            return 0;
        }
    }

    /**
     * 检测显卡显存容量（MB）
     * <p>
     * 需要 GL 上下文（仅在客户端渲染线程调用），优先使用 NVIDIA 的
     * {@code NVX_gpu_memory_info} 扩展查询总显存；AMD/ATI 的
     * {@code ATI_meminfo} 只暴露空闲显存，取三类空闲值中的最高者作为估算。
     *
     * @return 显存容量（MB），无法检测时返回0
     */
    public static int detectVramMB() {
        try {
            GLCapabilities capabilities = GL.getCapabilities();
            if (capabilities == null) {
                return 0;
            }
            if (capabilities.GL_NVX_gpu_memory_info) {
                // NVIDIA：总可用显存（KiB）
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer buffer = stack.mallocInt(1);
                    GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX, buffer);
                    return buffer.get(0) / 1024; // KiB -> MiB
                }
            } else if (capabilities.GL_ATI_meminfo) {
                // AMD/ATI：空闲显存（KiB），取三类中最高的作为估算
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer buffer = stack.mallocInt(1);
                    int maxFree = 0;
                    GL11.glGetIntegerv(ATIMeminfo.GL_VBO_FREE_MEMORY_ATI, buffer);
                    maxFree = Math.max(maxFree, buffer.get(0));
                    GL11.glGetIntegerv(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI, buffer);
                    maxFree = Math.max(maxFree, buffer.get(0));
                    GL11.glGetIntegerv(ATIMeminfo.GL_RENDERBUFFER_FREE_MEMORY_ATI, buffer);
                    maxFree = Math.max(maxFree, buffer.get(0));
                    return maxFree / 1024; // KiB -> MiB
                }
            }
        } catch (Throwable e) {
            // GL 上下文不可用或扩展不支持时返回0
        }
        return 0;
    }

    // 玩家持久化数据中的硬件信息键（性能遥测，供千兆内存之戒等使用）
    private static final String PLAYER_RAM_KEY = "PlayerPhysicalMemoryMB";
    private static final String PLAYER_VRAM_KEY = "PlayerVramMB";

    /**
     * 将玩家上报的硬件信息存入其持久化数据（按玩家区分，供服务端使用）。
     *
     * @param player           玩家实体
     * @param physicalMemoryMB 玩家设备物理内存容量（MB）
     * @param vramMB           玩家显卡显存容量（MB）
     */
    public static void setPlayerHardwareInfo(net.minecraft.world.entity.player.Player player, int physicalMemoryMB, int vramMB) {
        if (player == null) {
            return;
        }
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (data != null) {
            data.putInt(PLAYER_RAM_KEY, physicalMemoryMB);
            data.putInt(PLAYER_VRAM_KEY, vramMB);
        }
    }

    /**
     * 获取玩家上报的物理内存容量（MB）
     *
     * @return 物理内存容量（MB），未上报时返回0
     */
    public static int getPlayerPhysicalMemoryMB(net.minecraft.world.entity.player.Player player) {
        if (player == null || player.getPersistentData() == null) {
            return 0;
        }
        return player.getPersistentData().getInt(PLAYER_RAM_KEY);
    }

    /**
     * 获取玩家上报的显存容量（MB）
     *
     * @return 显存容量（MB），未上报时返回0
     */
    public static int getPlayerVramMB(net.minecraft.world.entity.player.Player player) {
        if (player == null || player.getPersistentData() == null) {
            return 0;
        }
        return player.getPersistentData().getInt(PLAYER_VRAM_KEY);
    }

    // 客户端本地缓存的本机硬件（用于 Tooltip 展示当前设备加成）
    private static Integer cachedPhysicalMemoryMB = null;
    private static Integer cachedVramMB = null;

    /**
     * 缓存客户端本机检测到的硬件信息（登录发送遥测包时调用）。
     *
     * @param physicalMemoryMB 物理内存容量（MB）
     * @param vramMB           显存容量（MB）
     */
    public static void cacheClientHardware(int physicalMemoryMB, int vramMB) {
        cachedPhysicalMemoryMB = physicalMemoryMB;
        cachedVramMB = vramMB;
    }

    /**
     * 获取客户端本机物理内存容量（MB），未缓存时按需检测。
     *
     * @return 物理内存容量（MB），获取失败时返回0
     */
    public static int getCachedPhysicalMemoryMB() {
        if (cachedPhysicalMemoryMB == null) {
            cachedPhysicalMemoryMB = detectPhysicalMemoryMB();
        }
        return cachedPhysicalMemoryMB;
    }

    /**
     * 获取客户端本机显存容量（MB），未缓存时按需检测。
     *
     * @return 显存容量（MB），获取失败时返回0
     */
    public static int getCachedVramMB() {
        if (cachedVramMB == null) {
            cachedVramMB = detectVramMB();
        }
        return cachedVramMB;
    }

    // 客户端本机 GPU 名称（用于 Tooltip 展示当前显卡，检测成功后缓存）
    private static String clientGpuName = null;

    /**
     * 获取客户端本机 GPU 名称（GL_RENDERER）。
     * <p>
     * 需要 GL 上下文（仅在客户端渲染线程调用）。首次调用成功时缓存，
     * 失败（上下文未就绪）时不缓存，下次调用会重试。
     *
     * @return GPU 名称，无法检测时返回 "Unknown"
     */
    public static String getClientGpuName() {
        if (clientGpuName == null) {
            try {
                String renderer = GL11.glGetString(GL11.GL_RENDERER);
                if (renderer != null && !renderer.isEmpty()) {
                    clientGpuName = renderer;
                }
            } catch (Throwable e) {
                // GL 上下文未就绪，稍后重试
            }
        }
        return clientGpuName != null ? clientGpuName : "Unknown";
    }

    // 存储各玩家上报的「应应用的属性结果」（按玩家区分，高频瞬态数据，用内存 Map 而非持久化 NBT）
    // 近视眼镜：攻击力修正（generic.attack_damage 的 MULTIPLY_BASE 修饰符数值）
    private static final Map<UUID, Double> playerGlassesBonusCache = new ConcurrentHashMap<>();

    /**
     * 存储玩家上报的近视眼镜攻击力修正。
     *
     * @param playerUuid 玩家 UUID
     * @param bonus      generic.attack_damage 的 MULTIPLY_BASE 修饰符数值
     */
    public static void setPlayerGlassesBonus(UUID playerUuid, double bonus) {
        if (playerUuid != null) {
            playerGlassesBonusCache.put(playerUuid, bonus);
        }
    }

    /**
     * 获取玩家上报的近视眼镜攻击力修正。
     *
     * @param playerUuid 玩家 UUID
     * @return 攻击力修正，未上报时返回 0（无加成）
     */
    public static double getPlayerGlassesBonus(UUID playerUuid) {
        return playerGlassesBonusCache.getOrDefault(playerUuid, 0.0);
    }

    /**
     * 移除玩家的近视眼镜攻击力修正（玩家登出时清理，防止内存泄漏）。
     *
     * @param playerUuid 玩家 UUID
     */
    public static void removePlayerGlassesBonus(UUID playerUuid) {
        if (playerUuid != null) {
            playerGlassesBonusCache.remove(playerUuid);
        }
    }

    // 深度求索之戒：攻击力修正（generic.attack_damage 的 MULTIPLY_BASE 修饰符数值）
    private static final Map<UUID, Double> playerDeepSeekBonusCache = new ConcurrentHashMap<>();

    /**
     * 存储玩家上报的 DeepSeek 之戒攻击力修正。
     *
     * @param playerUuid 玩家 UUID
     * @param bonus      generic.attack_damage 的 MULTIPLY_BASE 修饰符数值
     */
    public static void setPlayerDeepSeekBonus(UUID playerUuid, double bonus) {
        if (playerUuid != null) {
            playerDeepSeekBonusCache.put(playerUuid, bonus);
        }
    }

    /**
     * 获取玩家上报的 DeepSeek 之戒攻击力修正。
     *
     * @param playerUuid 玩家 UUID
     * @return 攻击力修正，未上报时返回 0（无加成）
     */
    public static double getPlayerDeepSeekBonus(UUID playerUuid) {
        return playerDeepSeekBonusCache.getOrDefault(playerUuid, 0.0);
    }

    /**
     * 移除玩家的 DeepSeek 之戒攻击力修正（玩家登出时清理，防止内存泄漏）。
     *
     * @param playerUuid 玩家 UUID
     */
    public static void removePlayerDeepSeekBonus(UUID playerUuid) {
        if (playerUuid != null) {
            playerDeepSeekBonusCache.remove(playerUuid);
        }
    }

    // 电竞牛头：应施加的状态效果掩码
    private static final Map<UUID, Integer> playerGamingCattleMaskCache = new ConcurrentHashMap<>();

    /**
     * 存储玩家上报的电竞牛头状态效果掩码。
     *
     * @param playerUuid 玩家 UUID
     * @param mask       应施加的效果位掩码（见 {@code GamingCattleItem} 位定义）
     */
    public static void setPlayerGamingCattleMask(UUID playerUuid, int mask) {
        if (playerUuid != null) {
            playerGamingCattleMaskCache.put(playerUuid, mask);
        }
    }

    /**
     * 获取玩家上报的电竞牛头状态效果掩码。
     *
     * @param playerUuid 玩家 UUID
     * @return 效果位掩码，未上报时返回 0（无效果）
     */
    public static int getPlayerGamingCattleMask(UUID playerUuid) {
        return playerGamingCattleMaskCache.getOrDefault(playerUuid, 0);
    }

    /**
     * 移除玩家的电竞牛头状态效果掩码（玩家登出时清理，防止内存泄漏）。
     *
     * @param playerUuid 玩家 UUID
     */
    public static void removePlayerGamingCattleMask(UUID playerUuid) {
        if (playerUuid != null) {
            playerGamingCattleMaskCache.remove(playerUuid);
        }
    }

    // 存储各玩家上报的已安装模组数（供 ATM 之戒按模组数加成，登录时上报一次）
    private static final Map<UUID, Integer> playerModCountCache = new ConcurrentHashMap<>();

    /**
     * 存储玩家上报的已安装模组数。
     *
     * @param playerUuid 玩家 UUID
     * @param modCount   已安装模组数
     */
    public static void setPlayerModCount(UUID playerUuid, int modCount) {
        if (playerUuid != null) {
            playerModCountCache.put(playerUuid, modCount);
        }
    }

    /**
     * 获取玩家上报的已安装模组数。
     *
     * @param playerUuid 玩家 UUID
     * @return 已安装模组数，未上报时返回0
     */
    public static int getPlayerModCount(UUID playerUuid) {
        return playerModCountCache.getOrDefault(playerUuid, 0);
    }

    /**
     * 移除玩家的模组数记录（玩家登出时清理，防止内存泄漏）。
     *
     * @param playerUuid 玩家 UUID
     */
    public static void removePlayerModCount(UUID playerUuid) {
        if (playerUuid != null) {
            playerModCountCache.remove(playerUuid);
        }
    }

    // 客户端检测到的最新 CPU 利用率（百分比），供 DeepSeek 之戒 tooltip 与上报使用（默认基线 40%）
    private static volatile int latestClientCpuLoad = 40;

    /**
     * 更新客户端检测到的最新 CPU 利用率（百分比）。
     *
     * @param percent 利用率百分比（0~100）
     */
    public static void setLatestClientCpuLoad(int percent) {
        latestClientCpuLoad = percent;
    }

    /**
     * 获取客户端检测到的最新 CPU 利用率（百分比）。
     *
     * @return 利用率百分比，未检测到时为基线 40%
     */
    public static int getLatestClientCpuLoad() {
        return latestClientCpuLoad;
    }

    // 客户端当前窗口分辨率（宽×高），供近视眼镜 tooltip 与上报使用（默认 1920x1080 基准）
    private static volatile int latestClientWindowWidth = 1920;
    private static volatile int latestClientWindowHeight = 1080;

    /**
     * 更新客户端当前窗口分辨率（像素）。
     *
     * @param width  窗口宽度
     * @param height 窗口高度
     */
    public static void setLatestClientWindowSize(int width, int height) {
        latestClientWindowWidth = width;
        latestClientWindowHeight = height;
    }

    /**
     * 获取客户端当前窗口宽度（像素）。
     *
     * @return 窗口宽度，未检测时默认 1920
     */
    public static int getLatestClientWindowWidth() {
        return latestClientWindowWidth;
    }

    /**
     * 获取客户端当前窗口高度（像素）。
     *
     * @return 窗口高度，未检测时默认 1080
     */
    public static int getLatestClientWindowHeight() {
        return latestClientWindowHeight;
    }

    /**
     * 获取当前客户端的CPU核心数
     *
     * @return CPU核心数
     */
    public static int getClientCPUCount() {
        if (EverlaArtifactsConfig.isPerformanceDebugMode()) {
            // 在调试模式下，使用配置的自定义值或生成随机数
            if (debugCPUCount == null) {
                // 检查是否设置了自定义CPU数量
                int customCPU = EverlaArtifactsConfig.getCustomDebugCPUCount();
                if (customCPU > 0) {
                    debugCPUCount = customCPU;
                } else {
                    // 如果没有设置自定义值，则生成随机数
                    debugCPUCount = 4 + random.nextInt(29); // 4 + [0-28] = [4-32]
                }
            }
            return debugCPUCount;
        } else {
            // 正常模式下返回真实CPU核心数
            debugCPUCount = null; // 清除调试值
            return Runtime.getRuntime().availableProcessors();
        }
    }
    
    /**
     * 获取当前客户端的分配内存（MB）
     *
     * @return 分配内存大小（MB）
     */
    public static int getClientAllocatedMemory() {
        if (EverlaArtifactsConfig.isPerformanceDebugMode()) {
            // 在调试模式下，使用配置的自定义值或生成随机数
            if (debugMemorySize == null) {
                // 检查是否设置了自定义内存大小
                int customMemory = EverlaArtifactsConfig.getCustomDebugMemorySize();
                if (customMemory > 0) {
                    debugMemorySize = customMemory;
                } else {
                    // 如果没有设置自定义值，则生成随机数
                    debugMemorySize = 4096 + random.nextInt(12289); // 4096 + [0-12288] = [4096-16384]
                }
            }
            return debugMemorySize;
        } else {
            // 正常模式下返回真实分配的内存
            debugMemorySize = null; // 清除调试值
            long maxMemory = Runtime.getRuntime().maxMemory();
            return (int) (maxMemory / (1024 * 1024)); // 转换为MB
        }
    }
}