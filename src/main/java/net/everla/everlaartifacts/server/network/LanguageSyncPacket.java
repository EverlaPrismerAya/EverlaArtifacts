package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.server.handlers.enchantment.ChineseCanFlyHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Client-to-server packet that sends the player's current language code
 * (e.g. {@code "zh_cn"}, {@code "en_us"}) on login.
 * <p>
 * Received by {@link ChineseCanFlyHandler} to determine whether the
 * player qualifies for the Chinese Can Fly enchantment's flight ability.
 */
public class LanguageSyncPacket {

    private final String languageCode;

    public LanguageSyncPacket(String languageCode) {
        this.languageCode = languageCode;
    }

    public LanguageSyncPacket(FriendlyByteBuf buffer) {
        this.languageCode = buffer.readUtf(16);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.languageCode, 16);
    }

    /**
     * Server-side handler: stores the language code and applies or
     * removes flight based on the current enchantment state.
     */
    public static void handle(LanguageSyncPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ChineseCanFlyHandler.onLanguageReceived(player, packet.languageCode);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Sends the player's language code from client to server.
     */
    public static void sendToServer(String languageCode) {
        EverlaartifactsMod.PACKET_HANDLER.sendToServer(
                new LanguageSyncPacket(languageCode));
    }
}
