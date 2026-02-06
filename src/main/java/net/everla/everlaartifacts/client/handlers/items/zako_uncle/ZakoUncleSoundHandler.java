package net.everla.everlaartifacts.client.handlers.items.zako_uncle;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;

/**
 * Zako Uncle物品音效处理器
 * 专门处理Zako Uncle物品使用时的客户端音效播放
 */
public class ZakoUncleSoundHandler {
    
    /**
     * 在客户端播放Zako Uncle音效
     * @param x X坐标
     * @param y Y坐标  
     * @param z Z坐标
     */
    public static void playZakoUncleSound(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            // 在客户端播放音效
            mc.level.playLocalSound(
                x, y, z,
                ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("everlaartifacts", "gfbhurt")),
                SoundSource.NEUTRAL,
                1.0F, 1.0F,
                false
            );
        }
    }
}