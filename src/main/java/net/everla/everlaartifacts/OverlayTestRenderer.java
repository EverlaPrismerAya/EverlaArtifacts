package net.everla.everlaartifacts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "everlaartifacts", value = Dist.CLIENT)
public class OverlayTestRenderer {
    // 覆盖显示状态
    private static final String DEFAULT_CONTENT = "文本测试"; // 固定文本
    private static int currentOffsetFromBottom = 0; // 从底部的偏移量（与RainbowNameHandler一致）
    private static long startTime = 0L;
    private static boolean isActive = false;
    
    // 持续时间：1秒 = 1000毫秒
    private static final long DISPLAY_DURATION = 1000L;

    /**
     * 从命令调用显示覆盖（使用从底部的偏移量，与RainbowNameHandler一致）
     */
    public static void showOverlay(int offsetFromBottom) {
        currentOffsetFromBottom = offsetFromBottom;
        startTime = System.currentTimeMillis();
        isActive = true;
    }

    /**
     * 每tick检查显示状态
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isActive) {
            return;
        }
        
        // 检查是否超过持续时间
        if (System.currentTimeMillis() - startTime > DISPLAY_DURATION) {
            isActive = false;
        }
    }

    /**
     * 渲染覆盖文本（使用screenHeight - offset的形式）
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!isActive) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        // 计算淡出效果
        long elapsed = System.currentTimeMillis() - startTime;
        int alpha;
        if (elapsed < 800) { // 前0.8秒完全显示
            alpha = 255;
        } else { // 最后0.2秒淡出
            long fadeTime = elapsed - 800;
            alpha = 255 - (int) ((fadeTime / 200.0) * 255);
            if (alpha < 0) alpha = 0;
        }
        
        if (alpha <= 0) {
            isActive = false;
            return;
        }
        
        // 渲染
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // 使用 screenHeight - offset 的形式（与RainbowNameHandler一致）
        int y = screenHeight - currentOffsetFromBottom;
        
        // 确保Y坐标在有效范围内
        if (y < 0) y = 0;
        if (y > screenHeight) y = screenHeight;
        
        // 固定显示"文本测试"
        Component textComponent = Component.literal(DEFAULT_CONTENT);
        int textWidth = mc.font.width(textComponent);
        int x = (screenWidth - textWidth) / 2;
        
        // 设置渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        // 使用较高 Z 值确保覆盖其他 UI
        poseStack.translate(0.0F, 0.0F, 999.0F);
        
        // 渲染文本（带透明度）
        int textColor = (alpha << 24) | 0xFFFFFF; // ARGB 格式
        guiGraphics.drawString(mc.font, textComponent, x, y, textColor, false);
        
        poseStack.popPose();
        RenderSystem.disableBlend();
    }
}