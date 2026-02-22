package net.everla.everlaartifacts.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.difficulty.DifficultyLevel;
import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.server.network.DifficultyChangePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 难度切换按钮GUI覆盖层
 * 在物品栏界面顶部显示难度按钮
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DifficultyButtonScreenOverlay {
    private static final ResourceLocation DIFFICULTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        EverlaartifactsMod.MODID, "textures/misc/difficulty.png");
    private static final ResourceLocation GFB_DIFFICULTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        EverlaartifactsMod.MODID, "textures/misc/gfb_difficulty.png");
    
    // 按钮基础属性（缩小50%）
    private static final int BUTTON_WIDTH = 72;  // 原来的50%
    private static final int BUTTON_HEIGHT = 16; // 原来的50%
    private static final int EXPANDED_BUTTON_HEIGHT = 16;
    
    // 当前状态
    private static boolean isExpanded = false;
    private static boolean isMainButtonVisible = true; // 控制主按钮可见性
    private static DifficultyLevel currentDifficulty = DifficultyLevel.NORMAL;
    private static long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 200; // 200ms点击冷却
    private static boolean isSpecialSeedWorld = false; // 是否为特殊种子世界
    
    // 展开菜单的位置和尺寸
    private static int menuX = 0;
    private static int menuY = 0;
    private static int menuWidth = 0;
    private static int menuHeight = 0;
    
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        Minecraft mc = Minecraft.getInstance();
        
        // 只在物品栏相关界面显示
        if (!(screen instanceof AbstractContainerScreen) && !(screen instanceof InventoryScreen)) {
            return;
        }
        
        // 检查是否为和平模式
        if (mc.level != null && mc.level.getDifficulty() == Difficulty.PEACEFUL) {
            return; // 和平模式下不显示按钮
        }
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();
        
        // 使用客户端同步的状态来确定当前难度
        String clientDifficultyName = EverlaartifactsMod.getClientDifficultyName();
        boolean isLunaticMode = EverlaartifactsMod.isClientLunaticMode();
        
        // 检查是否为特殊种子世界
        isSpecialSeedWorld = net.everla.everlaartifacts.server.network.DifficultySyncPacket.isClientSpecialSeedWorld();
        
        // 根据客户端状态设置当前难度
        try {
            Difficulty clientDifficulty = Difficulty.valueOf(clientDifficultyName);
            currentDifficulty = DifficultyLevel.fromVanillaDifficulty(clientDifficulty);
            
            // 特殊种子世界下强制显示为Extra难度
            if (isSpecialSeedWorld) {
                currentDifficulty = DifficultyLevel.EXTRA;
            }
            // 如果客户端标记为月狂模式，则强制显示为月狂（但不覆盖特殊种子世界的Extra难度）
            else if (isLunaticMode) {
                currentDifficulty = DifficultyLevel.LUNATIC;
            }
        } catch (IllegalArgumentException e) {
            // 如果无法解析难度名称，使用默认值
            currentDifficulty = DifficultyLevel.NORMAL;
        }
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // 主按钮位置（屏幕顶部中央）
        int mainButtonX = (screenWidth - BUTTON_WIDTH) / 2;
        int mainButtonY = 5;
        
        // 更新主按钮可见性状态
        isMainButtonVisible = !isExpanded;
        
        // 获取鼠标位置
        double mouseX = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getScreenHeight();
        
        // 如果展开状态，只渲染展开菜单
        if (isExpanded) {
            renderExpandedMenu(guiGraphics, mainButtonX, mainButtonY, poseStack);
            // 渲染展开菜单的tooltip
            renderExpandedMenuTooltip(guiGraphics, mouseX, mouseY);
        } else if (isMainButtonVisible) {
            // 否则渲染主按钮（仅在可见时）
            renderMainButton(guiGraphics, mainButtonX, mainButtonY, poseStack);
            // 渲染主按钮的tooltip
            if (isMouseOver(mouseX, mouseY, mainButtonX, mainButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                renderDifficultyTooltip(guiGraphics, currentDifficulty, mouseX, mouseY);
            }
        }
    }
    
    @SubscribeEvent
    public static void onScreenMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        Minecraft mc = Minecraft.getInstance();
        
        // 只处理物品栏相关界面
        if (!(screen instanceof AbstractContainerScreen) && !(screen instanceof InventoryScreen)) {
            return;
        }
        
        // 和平模式下不处理点击
        if (mc.level != null && mc.level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        long currentTime = System.currentTimeMillis();
        
        // 防止过快点击
        if (currentTime - lastClickTime < CLICK_COOLDOWN) {
            return;
        }
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int mainButtonX = (screenWidth - BUTTON_WIDTH) / 2;
        int mainButtonY = 5;
        
        // 检查是否点击主按钮（仅在主按钮可见时）
        if (isMainButtonVisible && isMouseOver(mouseX, mouseY, mainButtonX, mainButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            // 特殊种子世界禁用展开功能
            if (!isSpecialSeedWorld) {
                isExpanded = !isExpanded;
            }
            lastClickTime = currentTime;
            event.setCanceled(true);
            return;
        }
        
        // 如果已展开，检查子按钮点击
        if (isExpanded && isMouseOver(mouseX, mouseY, menuX, menuY, menuWidth, menuHeight)) {
            handleSubButtonClick(mouseX, mouseY);
            isExpanded = false;
            lastClickTime = currentTime;
            event.setCanceled(true);
        }
        // 点击其他区域收起菜单
        else if (isExpanded) {
            isExpanded = false;
            lastClickTime = currentTime;
        }
    }
    
    private static void renderMainButton(GuiGraphics guiGraphics, int x, int y, PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        if (isSpecialSeedWorld) {
            // 特殊种子世界：直接使用gfb_difficulty.png材质
            RenderSystem.setShaderTexture(0, GFB_DIFFICULTY_TEXTURE);
            // 直接渲染整个图片，不需要选择区域
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 1.0f);
            guiGraphics.blit(GFB_DIFFICULTY_TEXTURE, x * 2, y * 2, 0, 0, 144, 32, 144, 32);
            poseStack.popPose();
        } else {
            // 普通世界：使用原有逻辑
            RenderSystem.setShaderTexture(0, DIFFICULTY_TEXTURE);
            // 根据当前难度选择纹理位置
            int textureY = currentDifficulty.getTextureYOffset();
            
            // 使用缩放渲染，保持材质框选范围不变
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 1.0f);
            guiGraphics.blit(DIFFICULTY_TEXTURE, x * 2, y * 2, 0, textureY, 144, 32, 144, 128);
            poseStack.popPose();
        }
    }
    
    private static void renderExpandedMenu(GuiGraphics guiGraphics, int mainButtonX, int mainButtonY, PoseStack poseStack) {
        // 展开菜单位置就是主按钮的位置
        menuX = mainButtonX;
        menuY = mainButtonY;
        menuWidth = BUTTON_WIDTH + 20;
        menuHeight = BUTTON_HEIGHT * 4 + 25; // 4个按钮 + 间距
        
        // 渲染背景半透明遮罩
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0x80000000);
        
        // 渲染边框
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, 0xFFAAAAAA); // 上边框
        guiGraphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, 0xFFAAAAAA); // 下边框
        guiGraphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, 0xFFAAAAAA); // 左边框
        guiGraphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, 0xFFAAAAAA); // 右边框
        
        // 渲染四个难度按钮（纵向排列）
        int startX = menuX + 10;
        int startY = menuY + 5;
        
        DifficultyLevel[] levels = {DifficultyLevel.EASY, DifficultyLevel.NORMAL, DifficultyLevel.HARD, DifficultyLevel.LUNATIC};
        
        for (int i = 0; i < levels.length; i++) {
            int buttonY = startY + i * (BUTTON_HEIGHT + 5);
            int textureY = levels[i].getTextureYOffset();
            
            // 渲染按钮
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, DIFFICULTY_TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            
            // 使用缩放渲染，保持材质框选范围不变
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 1.0f);
            guiGraphics.blit(DIFFICULTY_TEXTURE, startX * 2, buttonY * 2, 0, textureY, 144, 32, 144, 128);
            poseStack.popPose();
        }
    }
    
    private static void handleSubButtonClick(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int startX = menuX + 10;
        int startY = menuY + 5;
        
        DifficultyLevel[] levels = {DifficultyLevel.EASY, DifficultyLevel.NORMAL, DifficultyLevel.HARD, DifficultyLevel.LUNATIC};
        
        for (int i = 0; i < levels.length; i++) {
            int buttonY = startY + i * (BUTTON_HEIGHT + 5);
            
            if (isMouseOver(mouseX, mouseY, startX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                DifficultyLevel selectedLevel = levels[i];
                
                // 发送网络包到服务端
                EverlaartifactsMod.PACKET_HANDLER.sendToServer(new DifficultyChangePacket(selectedLevel));
                
                // 在客户端播放切换音效（仅给切换者听）
                if (mc.player != null && mc.level != null) {
                    mc.player.playNotifySound(
                        net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                            ResourceLocation.fromNamespaceAndPath("everlaartifacts", "difficult_switch")
                        ),
                        SoundSource.MASTER,
                        1.0f,
                        1.0f
                    );
                }
                break;
            }
        }
    }
    
    private static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    private static void renderDifficultyTooltip(GuiGraphics guiGraphics, DifficultyLevel difficulty, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        String tooltipKey = "difficulty.everlaartifacts." + difficulty.name().toLowerCase() + ".desc";
        
        // 获取本地化文本
        String rawText = net.minecraft.client.resources.language.I18n.get(tooltipKey);
        
        // 处理换行符并创建多行tooltip
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
        if (rawText != null && !rawText.isEmpty()) {
            String[] lines = rawText.split("\\n");
            for (String line : lines) {
                tooltipLines.add(Component.literal(line));
            }
        }
        
        // 使用多行tooltip渲染方法
        if (!tooltipLines.isEmpty()) {
            guiGraphics.renderTooltip(mc.font, tooltipLines, java.util.Optional.empty(), (int)mouseX, (int)mouseY);
        }
    }
    
    private static void renderExpandedMenuTooltip(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int startX = menuX + 10;
        int startY = menuY + 5;
        
        DifficultyLevel[] levels = {DifficultyLevel.EASY, DifficultyLevel.NORMAL, DifficultyLevel.HARD, DifficultyLevel.LUNATIC};
        
        for (int i = 0; i < levels.length; i++) {
            int buttonY = startY + i * (BUTTON_HEIGHT + 5);
            
            if (isMouseOver(mouseX, mouseY, startX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                renderDifficultyTooltip(guiGraphics, levels[i], mouseX, mouseY);
                break;
            }
        }
    }
    
    /**
     * 强制收起菜单（可在其他地方调用）
     */
    public static void collapseMenu() {
        isExpanded = false;
    }
    
    /**
     * 获取当前难度
     */
    public static DifficultyLevel getCurrentDifficulty() {
        return currentDifficulty;
    }
}