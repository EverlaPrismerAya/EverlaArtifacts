package net.everla.everlaartifacts.client.screens;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import com.mojang.blaze3d.systems.RenderSystem;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class GenshinOverlayOverlay {
    private static final ResourceLocation OVERLAY_TEXTURE = 
        new ResourceLocation("everlaartifacts", "textures/screens/genshin_start.png");
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || player.level().isClientSide() == false) return;
        
        // 获取效果剩余时间（ticks）
        MobEffectInstance effect = player.getEffect(
            net.everla.everlaartifacts.init.EverlaartifactsModMobEffects.GENSHIN_START.get()
        );
        
        float alpha = 0.0f;
        if (effect != null) {
            int duration = effect.getDuration();
            if (duration > 40) {
                alpha = 1.0f; // 完全不透明（前N-2秒）
            } else {
                alpha = duration / 40.0f; // 最后2秒线性淡出 (40 ticks = 2秒)
            }
        }
        
        // 仅当透明度显著时渲染（避免渲染接近透明的图层）
        if (alpha > 0.01f) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            int w = event.getWindow().getGuiScaledWidth();
            int h = event.getWindow().getGuiScaledHeight();
            
            // 保存原始渲染状态
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            
            // 设置淡出透明度
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            
            // 渲染全屏覆盖
            guiGraphics.blit(OVERLAY_TEXTURE, 0, 0, 0, 0, w, h, w, h);
            
            // 恢复渲染状态
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }
}