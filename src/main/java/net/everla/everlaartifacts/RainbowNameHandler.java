package net.everla.everlaartifacts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "everlaartifacts", value = Dist.CLIENT)
public class RainbowNameHandler {
    private static float hueOffset = (System.currentTimeMillis() % 36000) / 100.0f;
    private static final TagKey<Item> RAINBOW_NAME_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.tryParse("everlaartifacts:rainbow_name"));
    
    private static ItemStack lastStack = ItemStack.EMPTY;
    private static long lastSwitchTime = 0L;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != null) {
            hueOffset = (hueOffset + 0.6f) % 360.0f;
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.is(RAINBOW_NAME_TAG)) return;

        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;

        String rawName = tooltip.get(0).getString();
        if (rawName.isEmpty()) return;

        // 使用统一彩虹处理器（字符偏移5.0f）
        MutableComponent rainbowName = EverlaRainbowHandler.buildRainbowComponent(rawName, hueOffset, 5.0f);
        if (rainbowName != null && !rainbowName.getString().isEmpty()) {
            tooltip.set(0, rainbowName);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack mainHand = mc.player.getMainHandItem();
        if (!ItemStack.matches(lastStack, mainHand)) {
            lastStack = mainHand.copy();
            lastSwitchTime = System.currentTimeMillis();
        }
        if (mainHand.isEmpty() || !mainHand.is(RAINBOW_NAME_TAG)) return;

        Component cleanName = mainHand.getHoverName();
        if (cleanName.getString().isEmpty()) return;

        // 使用统一彩虹处理器（字符偏移5.0f）
        MutableComponent rainbowName = EverlaRainbowHandler.buildRainbowComponent(cleanName.getString(), hueOffset, 5.0f);
        if (rainbowName == null || rainbowName.getString().isEmpty()) return;

        // 淡出效果计算
        long holdTime = System.currentTimeMillis() - lastSwitchTime;
        int alpha;
        if (holdTime < 1000) alpha = 255;
        else if (holdTime < 2000) alpha = 255 - (int)((holdTime - 1000) * 255 / 1000);
        else return;
        if (alpha <= 0) return;

        // 渲染
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int nameWidth = mc.font.width(rainbowName);
        int x = (screenWidth - nameWidth) / 2;
        int y = screenHeight - 45;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 1000.0F); // 覆盖原生中括号

        int color = (alpha << 24) | 0xFFFFFF;
        guiGraphics.drawString(mc.font, rainbowName, x, y, color, false);

        poseStack.popPose();
        RenderSystem.enableDepthTest();
    }
}