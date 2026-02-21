package net.everla.everlaartifacts.server;

import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;

import java.util.Random;

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