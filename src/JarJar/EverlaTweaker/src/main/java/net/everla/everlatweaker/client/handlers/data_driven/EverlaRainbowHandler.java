package net.everla.everlatweaker.client.handlers.data_driven;

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
        boolean useRainbow = true; // 控制是否使用彩虹色
        Integer fixedColor = null; // 固定颜色值
        int charIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(++i);
                if (code == 'r') {
                    // §r完全重置：清除所有格式并回到彩虹模式
                    bold = italic = underline = strikethrough = obfuscated = false;
                    useRainbow = true;
                    fixedColor = null;
                    continue; // 处理完后继续循环
                } else switch (code) {
                    case 'l' -> bold = true;
                    case 'o' -> italic = true;
                    case 'n' -> underline = true;
                    case 'm' -> strikethrough = true;
                    case 'k' -> obfuscated = true;
                    // 颜色代码处理（0-9, a-f, A-F）
                    case '0' -> { useRainbow = false; fixedColor = 0x000000; } // 黑色
                    case '1' -> { useRainbow = false; fixedColor = 0x0000AA; } // 深蓝
                    case '2' -> { useRainbow = false; fixedColor = 0x00AA00; } // 深绿
                    case '3' -> { useRainbow = false; fixedColor = 0x00AAAA; } // 青色
                    case '4' -> { useRainbow = false; fixedColor = 0xAA0000; } // 深红
                    case '5' -> { useRainbow = false; fixedColor = 0xAA00AA; } // 紫色
                    case '6' -> { useRainbow = false; fixedColor = 0xFFAA00; } // 金色
                    case '7' -> { useRainbow = false; fixedColor = 0xAAAAAA; } // 灰色
                    case '8' -> { useRainbow = false; fixedColor = 0x555555; } // 深灰
                    case '9' -> { useRainbow = false; fixedColor = 0x5555FF; } // 蓝色
                    case 'a', 'A' -> { useRainbow = false; fixedColor = 0x55FF55; } // 绿色
                    case 'b', 'B' -> { useRainbow = false; fixedColor = 0x55FFFF; } // 天蓝色
                    case 'c', 'C' -> { useRainbow = false; fixedColor = 0xFF5555; } // 红色
                    case 'd', 'D' -> { useRainbow = false; fixedColor = 0xFF55FF; } // 粉色
                    case 'e', 'E' -> { useRainbow = false; fixedColor = 0xFFFF55; } // 黄色
                    case 'f', 'F' -> { useRainbow = false; fixedColor = 0xFFFFFF; } // 白色
                    default -> {}
                }
                continue; // 遇到格式代码时，不递增charIndex
            }
            // 只处理换行相关的控制字符
            if (c == '\r') continue; // 忽略回车符
            if (c == '\n') {
                // 保留换行符，使用当前颜色和格式
                int currentColor = useRainbow ? 
                    hsvToRgb(((baseHue + charIndex * charHueOffset) % 360.0f + 360.0f) % 360.0f, 1.0f, 1.0f) : 
                    (fixedColor != null ? fixedColor : 0xFFFFFF);
                
                Style lineBreakStyle = Style.EMPTY
                    .withColor(currentColor)
                    .withBold(bold)
                    .withItalic(italic)
                    .withUnderlined(underline)
                    .withStrikethrough(strikethrough)
                    .withObfuscated(obfuscated);
                
                result.append(Component.literal("\n").withStyle(lineBreakStyle));
                if (useRainbow) charIndex++; // 只有在彩虹模式下才递增索引
                continue;
            }
            // 注意：普通空格和其他空白字符会正常处理，不在此处过滤

            // 确定字符颜色
            int charColor;
            if (useRainbow) {
                // 使用彩虹色
                float charHue = ((baseHue + charIndex * charHueOffset) % 360.0f + 360.0f) % 360.0f; // 确保结果为正
                charColor = hsvToRgb(charHue, 1.0f, 1.0f);
                charIndex++;
            } else {
                // 使用固定颜色（确保有默认值）
                charColor = (fixedColor != null) ? fixedColor : 0xFFFFFF; // 默认白色
            }

            Style style = Style.EMPTY
                .withColor(charColor)
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
