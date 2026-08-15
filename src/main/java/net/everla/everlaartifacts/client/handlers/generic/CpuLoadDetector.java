package net.everla.everlaartifacts.client.handlers.generic;

import net.everla.everlaartifacts.server.PerformanceMetrics;

import java.lang.management.ManagementFactory;

/**
 * 客户端 CPU 利用率检测器（DeepSeek 之戒）。
 * <p>
 * 通过 JDK 的 {@link com.sun.management.OperatingSystemMXBean#getSystemCpuLoad()}
 * 读取系统 CPU 利用率（0.0~1.0，首次调用或不可用时返回 -1.0）。后台线程每 10 秒
 * 轮询一次并缓存为百分比，避免阻塞渲染线程；玩家每 40 刻上报一次缓存的利用率到服务端。
 * <p>
 * 读取失败或不可用时保持当前缓存值（默认基线 40%，即 0% 加成）。
 */
public class CpuLoadDetector {

    /** 利用率轮询间隔（毫秒）。CPU 利用率变化较快，10 秒足够平稳 */
    private static final long POLL_INTERVAL_MS = 10_000L;

    private static volatile boolean running = false;
    private static Thread thread = null;

    /** 启动后台利用率轮询线程（幂等，客户端登录时调用） */
    public static void start() {
        synchronized (CpuLoadDetector.class) {
            if (running) {
                return;
            }
            running = true;
            thread = new Thread(CpuLoadDetector::pollLoop, "everla-deepseek-cpu-load");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /** 停止轮询线程（客户端登出时调用） */
    public static void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    private static void pollLoop() {
        while (running) {
            int percent = queryCpuLoadPercent();
            if (percent >= 0) {
                PerformanceMetrics.setLatestClientCpuLoad(percent);
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * 读取系统 CPU 利用率（百分比 0~100）。
     *
     * @return 利用率百分比，不可用返回 -1
     */
    private static int queryCpuLoadPercent() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double load = os.getSystemCpuLoad(); // 0.0~1.0，首次调用/不可用为 -1.0
            if (load >= 0.0 && load <= 1.0) {
                return (int) Math.round(load * 100.0);
            }
        } catch (Throwable e) {
            // 读取失败返回 -1（保持上次缓存值）
        }
        return -1;
    }
}
