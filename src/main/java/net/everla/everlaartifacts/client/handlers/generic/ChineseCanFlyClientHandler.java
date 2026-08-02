package net.everla.everlaartifacts.client.handlers.generic;

import net.everla.everlaartifacts.server.network.LanguageSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Chinese Can Fly enchantment.
 * <p>
 * Detects the player's Minecraft language on login and sends it to the
 * server via {@link LanguageSyncPacket} so the server can determine
 * whether to grant flight.
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", value = Dist.CLIENT)
public final class ChineseCanFlyClientHandler {

    @SubscribeEvent
    public static void onClientLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        // Read the current language code from client options
        String langCode = Minecraft.getInstance().options.languageCode;

        if (langCode != null && !langCode.isEmpty()) {
            LanguageSyncPacket.sendToServer(langCode.toLowerCase());
        }
    }
}
