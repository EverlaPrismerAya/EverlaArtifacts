package net.everla.everlaartifacts.client.screens;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BloodBlossomScreenOverlay {
    private static final ResourceLocation BLOOD_BLOSSOM_TEXTURE = new ResourceLocation("everlaartifacts", "textures/particle/blood_blossom.png");
    
    // 存储从服务端接收到的世界坐标，使用独立的插值系统
    private static final Map<UUID, SmoothPositionInterpolator> interpolatedPositions = new HashMap<>();
    
    public static class SmoothPositionInterpolator {
        // 实际接收的坐标（服务端发送的）
        private double actualX, actualY, actualZ;
        
        // 渲染坐标（用于渲染，与实际坐标分离）
        private double renderX, renderY, renderZ;
        
        // 插值因子，控制平滑过渡速度
        private static final double INTERPOLATION_FACTOR = 0.011; // 较慢的插值速度，适应每秒4次发包
        
        // 视锥内外状态
        private boolean inViewFrustum;
        
        // 最后接收时间
        private long lastReceivedTime;
        
        public SmoothPositionInterpolator(double x, double y, double z) {
            this.actualX = this.renderX = x;
            this.actualY = this.renderY = y;
            this.actualZ = this.renderZ = z;
            this.inViewFrustum = false;
            this.lastReceivedTime = System.currentTimeMillis();
        }
        
        public void updateActualPosition(double x, double y, double z) {
            // 更新实际坐标
            this.actualX = x;
            this.actualY = y;
            this.actualZ = z;
            this.lastReceivedTime = System.currentTimeMillis();
        }
        
        public void updateInterpolation() {
            // 使用简单的线性插值从当前渲染坐标平滑过渡到实际坐标
            this.renderX += (this.actualX - this.renderX) * INTERPOLATION_FACTOR;
            this.renderY += (this.actualY - this.renderY) * INTERPOLATION_FACTOR;
            this.renderZ += (this.actualZ - this.renderZ) * INTERPOLATION_FACTOR;
        }
        
        public double getRenderX() {
            return renderX;
        }
        
        public double getRenderY() {
            return renderY;
        }
        
        public double getRenderZ() {
            return renderZ;
        }
        
        public void setInViewFrustum(boolean inView) {
            this.inViewFrustum = inView;
        }
        
        public boolean isInViewFrustum() {
            return inViewFrustum;
        }
        
        public long getLastReceivedTime() {
            return lastReceivedTime;
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent event) {
        if (event.getOverlay() != net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HELMET.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        // 首先更新所有实体的插值坐标
        for (Map.Entry<UUID, SmoothPositionInterpolator> entry : interpolatedPositions.entrySet()) {
            entry.getValue().updateInterpolation();
        }

        // 视锥剔除：只渲染视锥内的实体
        for (Map.Entry<UUID, SmoothPositionInterpolator> entry : interpolatedPositions.entrySet()) {
            SmoothPositionInterpolator interpolator = entry.getValue();
            
            // 使用渲染坐标进行视锥剔除检查
            double renderX = interpolator.getRenderX();
            double renderY = interpolator.getRenderY();
            double renderZ = interpolator.getRenderZ();
            
            // 判断实体是否在视锥内
            boolean inFrustum = isInViewFrustum(renderX, renderY, renderZ, mc);
            interpolator.setInViewFrustum(inFrustum);
        }

        // 只渲染视锥内的实体
        for (Map.Entry<UUID, SmoothPositionInterpolator> entry : interpolatedPositions.entrySet()) {
            SmoothPositionInterpolator interpolator = entry.getValue();
            
            // 如果不在视锥内，跳过渲染
            if (!interpolator.isInViewFrustum()) {
                continue;
            }
            
            // 使用渲染坐标（经过插值的坐标）进行屏幕坐标转换
            double renderX = interpolator.getRenderX();
            double renderY = interpolator.getRenderY();
            double renderZ = interpolator.getRenderZ();

            // 使用Minecraft的官方方式将世界坐标转换为屏幕坐标
            int screenX = -1;
            int screenY = -1;
            
            try {
                // 将世界坐标转换为屏幕坐标
                double[] screenCoords = worldToScreen(renderX, renderY, renderZ, mc);
                
                if (screenCoords != null) {
                    screenX = (int) screenCoords[0];
                    screenY = (int) screenCoords[1];
                    
                    // 确保在屏幕范围内
                    if (screenX < -16 || screenX > screenWidth + 16 || screenY < -16 || screenY > screenHeight + 16) {
                        continue;
                    }
                } else {
                    continue; // 转换失败
                }
            } catch (Exception e) {
                continue; // 转换失败则跳过
            }

            // 使用游戏时间和游戏刻度来计算旋转角度，60 RPM = 每秒一圈 = 360度/秒
            // 使用Minecraft的游戏刻度
            long gameTime = mc.level != null ? mc.level.getGameTime() : 0;
            float gamePartialTicks = mc.getFrameTime(); // 使用帧间插值时间
            
            // 60 RPM = 1转/秒 = 360度/秒
            float rotationAngle = (gameTime * 1.2f + gamePartialTicks * 1.2f) % 360.0f; // 0.6 = 360度/60ticks，因为Minecraft每秒20 ticks
            
            // 计算距离以确定缩放大小（近大远小）
            double distance = calculateDistanceToCamera(renderX, renderY, renderZ, mc);
            
            renderBloodBlossomAtPosition(event.getGuiGraphics(), screenX, screenY, screenWidth, screenHeight, rotationAngle, distance);
        }
    }

    /**
     * 检查坐标是否在视锥内
     */
    private static boolean isInViewFrustum(double x, double y, double z, Minecraft mc) {
        if (mc.player == null || mc.gameRenderer.getMainCamera() == null) {
            return false;
        }
        
        // 获取相机位置（玩家眼睛位置）
        double cameraX = mc.gameRenderer.getMainCamera().getPosition().x;
        double cameraY = mc.gameRenderer.getMainCamera().getPosition().y;
        double cameraZ = mc.gameRenderer.getMainCamera().getPosition().z;
        
        // 计算距离（可以根据需要调整视距）
        double dx = x - cameraX;
        double dy = y - cameraY;
        double dz = z - cameraZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        
        // 限制渲染距离（可根据需要调整）
        final double MAX_RENDER_DISTANCE_SQUARED = 64.0 * 64.0; // 64格距离
        
        return distanceSquared <= MAX_RENDER_DISTANCE_SQUARED;
    }
    
    /**
     * 计算实体到相机的距离
     */
    private static double calculateDistanceToCamera(double x, double y, double z, Minecraft mc) {
        double cameraX = mc.gameRenderer.getMainCamera().getPosition().x;
        double cameraY = mc.gameRenderer.getMainCamera().getPosition().y;
        double cameraZ = mc.gameRenderer.getMainCamera().getPosition().z;
        
        double dx = x - cameraX;
        double dy = y - cameraY;
        double dz = z - cameraZ;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * 将世界坐标转换为屏幕坐标
     */
    private static double[] worldToScreen(double worldX, double worldY, double worldZ, Minecraft mc) {
        // 获取相机位置（玩家眼睛位置）
        double cameraX = mc.gameRenderer.getMainCamera().getPosition().x;
        double cameraY = mc.gameRenderer.getMainCamera().getPosition().y;
        double cameraZ = mc.gameRenderer.getMainCamera().getPosition().z;
        
        // 相对位置（从相机到目标点）
        double dx = worldX - cameraX;
        double dy = worldY - cameraY;
        double dz = worldZ - cameraZ;
        
        // 获取玩家视角
        double pitch = Math.toRadians(mc.player.getXRot());
        double yaw = Math.toRadians(mc.player.getYRot());
        
        // 将世界坐标转换到相机坐标系
        // 首先绕Y轴旋转(-yaw)，然后绕X轴旋转(-pitch)
        
        // 绕Y轴旋转（应用逆变换，即-yaw）
        double cos_yaw = Math.cos(-yaw);
        double sin_yaw = Math.sin(-yaw);
        
        // 应用旋转矩阵
        // x' = x*cos - z*sin
        // z' = x*sin + z*cos
        // y' = y (y坐标不受Y轴旋转影响)
        double x_rot = dx * cos_yaw - dz * sin_yaw;
        double z_rot = dx * sin_yaw + dz * cos_yaw;
        double y_rot = dy;
        
        // 绕X轴旋转（应用逆变换，即-pitch）
        double cos_pitch = Math.cos(-pitch);
        double sin_pitch = Math.sin(-pitch);
        
        // 应用旋转矩阵
        // y'' = y'*cos - z'*sin
        // z'' = y'*sin + z'*cos
        // x'' = x' (x坐标不受X轴旋转影响)
        double y_cam = y_rot * cos_pitch - z_rot * sin_pitch;
        double z_cam = y_rot * sin_pitch + z_rot * cos_pitch;
        double x_cam = x_rot;
        
        // 检查点是否在相机前方
        if (z_cam <= 0.1) {
            return null; // 点在相机后方，不可见
        }
        
        // 获取窗口尺寸
        Window window = mc.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        
        // 计算FOV参数
        float fov = mc.options.fov().get().floatValue();
        double tan_half_fov = Math.tan(Math.toRadians(fov / 2.0));
        
        // 透视投影
        // 将相机坐标转换为屏幕坐标
        // 修正X轴方向 - 这里应该是负号来纠正X轴方向
        double screenX = -(x_cam / z_cam) * (width / 4.0) / tan_half_fov;
        double screenY = (y_cam / z_cam) * (height / 2.0) / tan_half_fov;
        
        // 转换为屏幕坐标系（原点在左上角）
        screenX += width / 2.0;
        screenY = height / 2.0 - screenY; // Y轴翻转
        
        return new double[]{screenX, screenY};
    }

    private static void renderBloodBlossomAtPosition(GuiGraphics guiGraphics, int x, int y, int screenWidth, int screenHeight, float rotationAngle, double distance) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 设置75%透明度
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.75F);
        
        // 计算缩放比例，基于距离实现近大远小
        // 距离越近，贴图越大；距离越远，贴图越小
        double sizeScale = Math.max(0.5, 3.0 / distance); // 基础大小除以距离，设置最小缩放为0.5
        
        // 使用PoseStack进行旋转变换和缩放
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        // 先移动到目标屏幕位置
        poseStack.translate(x, y, 0);
        
        // 应用缩放
        poseStack.scale((float)sizeScale, (float)sizeScale, 1.0F);
        
        // 应用旋转（绕中心点旋转）- 必须在缩放之后
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationAngle));
        
        // 绘制旋转后的贴图，以中心点为原点绘制
        // 因为已经translate到了目标位置，所以绘制时需要以相对于中心点的坐标
        guiGraphics.blit(BLOOD_BLOSSOM_TEXTURE, -32, -32, 0, 0, 64, 64, 64, 64);
        
        poseStack.popPose();
        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public static void addWorldPosition(UUID uuid, double x, double y, double z) {
        if (!interpolatedPositions.containsKey(uuid)) {
            interpolatedPositions.put(uuid, new SmoothPositionInterpolator(x, y, z));
        } else {
            interpolatedPositions.get(uuid).updateActualPosition(x, y, z);
        }
    }

    public static void clearWorldPositions() {
        // 不再清除所有位置，而是保留现有位置
        // 这样可以让插值继续进行
    }
    
    // 添加方法来清理长时间未更新的实体
    public static void cleanupOldPositions() {
        long currentTime = System.currentTimeMillis();
        interpolatedPositions.entrySet().removeIf(entry -> {
            SmoothPositionInterpolator interpolator = entry.getValue();
            // 如果超过1.5秒没有收到该实体的更新，则认为实体已消失（可能死亡或离开范围）
            return currentTime - interpolator.getLastReceivedTime() > 1500;
        });
    }
    
    // 添加方法来移除特定实体（例如当实体死亡时）
    public static void removeEntity(UUID uuid) {
        interpolatedPositions.remove(uuid);
    }
}