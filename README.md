# EverlaArtifacts

一个关于作者胡思乱想的 Forge 1.20.1 模组。

## 构建方式

依赖 **Java 17**，项目根目录下执行：

```bash
./gradlew build          # 构建模组 jar（会自动构建并内嵌 EverlaTweaker）
./gradlew :everlatweaker:build     #只构建EverlaTweaker
./gradlew runClient      # 启动 Minecraft 客户端
./gradlew runServer      # 启动测试服务器
```

构建产物位于 `build/libs/`，**分发请用 `-all` 后缀的 jar**（内含 Jar-in-Jar 内嵌的依赖）：

| 产物 | 说明 |
|---|---|
| `EverlaArtifacts-<版本>-forge-1.20.1-all.jar` | **分发包**（内含内嵌模组） |
| `EverlaArtifacts-<版本>-forge-1.20.1.jar` | 不含内嵌模组的精简包 |

### Jar-in-Jar 子项目：EverlaTweaker

数据驱动系统（彩虹名称、彩虹物品描述、物品防火、物品防爆、物品不可破坏）位于 Jar-in-Jar 子项目
`src/JarJar/EverlaTweaker`（modId `everlatweaker`）。它随根项目一起构建、被自动内嵌进 `-all` jar，
**无需单独构建或安装**；只装 EverlaArtifacts 即可获得全部功能。

单独构建该子项目：`./gradlew :everlatweaker:build`（产物在 `src/JarJar/EverlaTweaker/build/libs/`）。

> 注意：改 EverlaTweaker 源码后直接重新 `./gradlew build` 即可，构建会自动重新编译并内嵌最新代码。

### 独立模组：EverlaDiscs

音乐唱片（约 29 张）为**独立模组** `EverlaDiscs`（modId `everladiscs`），协议All Rights Reserved，
**不内嵌**。两个模组无强制依赖。

### 依赖

- **EverlaTweaker** — 必装（Jar-in-Jar 内嵌，自动提供）
- **JEI** — 可选
- **Curios API** — 可选

## 开源声明

本项目使用了以下开源软件/代码：

https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。

版权所有者：(c) 2024-2026 Nova-Committee
