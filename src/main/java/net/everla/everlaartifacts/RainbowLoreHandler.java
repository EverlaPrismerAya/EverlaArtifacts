package net.everla.everlaartifacts;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = "everlaartifacts", value = Dist.CLIENT)
public class RainbowLoreHandler {
    private static float hueOffset = 0.0f;
    private static final TagKey<Item> RAINBOW_LORE_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.tryParse("everlaartifacts:rainbow_lore"));

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
        Player player = event.getEntity();
        if (player == null || !player.level().isClientSide()) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(RAINBOW_LORE_TAG)) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;

        String translationKey = String.format("text.rainbow_lore.%s.%s", itemId.getNamespace(), itemId.getPath());
        String rawText = Component.translatable(translationKey).getString();
        if (rawText.equals(translationKey)) return; // 未找到翻译

        // 分割多行（支持多种换行符）
        String[] lines = rawText.split("\\\\n|\\\\r|\\\\r\\\\n|\n|\r\n?");
        List<Component> tooltip = event.getToolTip();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // 每行基础色相 = 全局偏移 + 行偏移(10°)
            float baseHue = (hueOffset + i * 10) % 360.0f;
            // 使用统一彩虹处理器（字符偏移4.0f）
            MutableComponent rainbowLine = EverlaRainbowHandler.buildRainbowComponent(line, baseHue, 4.0f);
            
            if (rainbowLine != null && !rainbowLine.getString().isEmpty()) {
                tooltip.add(rainbowLine);
            }
        }
    }
}