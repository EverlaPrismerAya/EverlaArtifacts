package net.everla.everlaartifacts;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * 彩虹效果核心处理器（无状态工具类）
 * 负责：HSV→RGB转换、§格式解析、字符级彩虹生成
 */
public final class EverlaRainbowHandler {
    private EverlaRainbowHandler() {
        // 工具类禁止实例化
    }

    /**
     * 生成彩虹文本组件（保留§格式代码解析）
     * @param text 原始文本（含§代码）
     * @param baseHue 基础色相（0-360）
     * @param charHueOffset 每个字符的色相偏移量（推荐 4.0f~5.0f）
     * @return 彩虹格式化后的MutableComponent（颜色为0xRRGGBB，不含Alpha）
     */
    public static MutableComponent buildRainbowComponent(String text, float baseHue, float charHueOffset) {
        if (text == null || text.isEmpty()) {
            return Component.empty(); // 返回空组件而不是null，避免NPE
        }

        // 参数验证和规范化
        baseHue = Math.max(0.0f, Math.min(360.0f, baseHue));
        charHueOffset = Math.max(-360.0f, Math.min(360.0f, charHueOffset));

        MutableComponent result = Component.empty();
        boolean bold = false, italic = false, underline = false, strikethrough = false, obfuscated = false;
        int charIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(++i);
                if (code == 'r') {
                    bold = italic = underline = strikethrough = obfuscated = false;
                } else switch (code) {
                    case 'l' -> bold = true;
                    case 'o' -> italic = true;
                    case 'n' -> underline = true;
                    case 'm' -> strikethrough = true;
                    case 'k' -> obfuscated = true;
                    // 颜色代码（0-9, a-f）被彩虹色覆盖，直接忽略
                    default -> {}
                }
                continue; // 遇到格式代码时，不递增charIndex
            }
            if (c == '\r' || c == '\n') continue; // 跳过换行符（由调用方处理多行）

            // 字符级色相偏移
            float charHue = ((baseHue + charIndex * charHueOffset) % 360.0f + 360.0f) % 360.0f; // 确保结果为正
            int rgb = hsvToRgb(charHue, 1.0f, 1.0f);
            charIndex++;

            Style style = Style.EMPTY
                .withColor(rgb)
                .withBold(bold)
                .withItalic(italic)
                .withUnderlined(underline)
                .withStrikethrough(strikethrough)
                .withObfuscated(obfuscated);

            result.append(Component.literal(String.valueOf(c)).withStyle(style));
        }
        return result;
    }

    /**
     * HSV 转 RGB（返回 0xRRGGBB 格式）
     * @param hue 色相 (0-360)
     * @param saturation 饱和度 (0.0-1.0)
     * @param value 亮度 (0.0-1.0)
     * @return RGB 颜色值
     */
    public static int hsvToRgb(float hue, float saturation, float value) {
        // 规范化色相值到[0, 360)范围
        hue = ((hue % 360.0f) + 360.0f) % 360.0f;

        float c = value * saturation;
        float x = c * (1 - Math.abs((hue / 60.0f) % 2 - 1));
        float m = value - c;

        float r, g, b;
        if (hue < 60) { r = c; g = x; b = 0; }
        else if (hue < 120) { r = x; g = c; b = 0; }
        else if (hue < 180) { r = 0; g = c; b = x; }
        else if (hue < 240) { r = 0; g = x; b = c; }
        else if (hue < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        int red = Math.max(0, Math.min(255, (int) Math.round((r + m) * 255)));
        int green = Math.max(0, Math.min(255, (int) Math.round((g + m) * 255)));
        int blue = Math.max(0, Math.min(255, (int) Math.round((b + m) * 255)));
        return (red << 16) | (green << 8) | blue;
    }
}
