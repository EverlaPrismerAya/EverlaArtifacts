package net.everla.everlaartifacts;

import net.minecraftforge.fml.loading.FMLEnvironment;
import net.everla.everlaartifacts.config.EverlaArtifactsConfig;

import java.util.Random;

/**
 * 性能指标计算和网络传输类
 */
public class PerformanceMetrics {
    // 评分函数参数
    private static final double KC = 0.1;  // CPU评分曲线系数
    private static final int C0 = 4;       // CPU评分中点
    private static final double KM = 0.005; // 内存评分曲线系数
    private static final int M0 = 2048;    // 内存评分中点 (MB)
    private static final int WC = 50;      // CPU权重
    private static final int WM = 50;      // 内存权重
    
    private static final Random random = new Random();
    
    // 存储调试模式下的固定随机数
    private static Integer debugCPUCount = null;
    private static Integer debugMemorySize = null;
    
    // 存储从服务端接收的性能评分
    private static Double serverPerformanceScore = null;

    /**
     * CPU评分函数: F(c) = 100 / (1 + e^(-kc * (c - c0)))
     *
     * @param cpuCore CPU核心数
     * @return CPU评分
     */
    public static double calculateCPUScore(int cpuCore) {
        double exponent = -KC * (cpuCore - C0);
        return 100.0 / (1 + Math.exp(exponent));
    }

    /**
     * 内存评分函数: G(m) = 100 / (1 + e^(-km * (m - m0)))
     *
     * @param allocatedMemory 分配的内存大小 (MB)
     * @return 内存评分
     */
    public static double calculateMemoryScore(int allocatedMemory) {
        double exponent = -KM * (allocatedMemory - M0);
        return 100.0 / (1 + Math.exp(exponent));
    }

    /**
     * 总评分函数: S(c,m) = (wc * F(c) + wm * G(m)) / 100
     *
     * @param cpuCore         CPU核心数
     * @param allocatedMemory 分配的内存大小 (MB)
     * @return 总评分
     */
    public static double calculateTotalScore(int cpuCore, int allocatedMemory) {
        double cpuScore = calculateCPUScore(cpuCore);
        double memoryScore = calculateMemoryScore(allocatedMemory);
        return (WC * cpuScore + WM * memoryScore) / 100.0;
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