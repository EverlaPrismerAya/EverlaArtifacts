package net.everla.everlaartifacts.server.handlers.enchantment;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.mixin.AbilitiesAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for the "中国人能飞" (Chinese Can Fly) enchantment.
 * <p>
 * <b>Flow:</b>
 * <ol>
 *   <li>On login, the client detects its language code and sends a
 *       {@link net.everla.everlaartifacts.server.network.LanguageSyncPacket}
 *       to the server.</li>
 *   <li>The server stores the language code in {@link #PLAYER_LANGUAGES}.</li>
 *   <li>Every tick, and on equipment change, the server checks whether the
 *       player has a zh-variant language AND the enchantment on their chest
 *       armour. If both conditions are met, creative flight is enabled at
 *       half speed.</li>
 *   <li>When conditions are no longer met, flight is revoked (unless the
 *       player is in creative/spectator mode).</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChineseCanFlyHandler {

    /** zh-variant language code prefixes that qualify for flight. */
    private static final String[] ZH_PREFIXES = {"zh_cn", "zh_tw", "zh_hk", "zh_sg", "zh_mo", "zh"};

    /** Half of the creative-mode default flying speed (0.05 → 0.025). */
    private static final float HALF_CREATIVE_FLY_SPEED = 0.025F;

    /** Maps player UUID → language code reported by the client. */
    static final Map<UUID, String> PLAYER_LANGUAGES = new ConcurrentHashMap<>();

    /** Tracks which players currently have enchantment-provided flight active. */
    private static final Set<UUID> ENCHANTMENT_FLYING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // ── Called by the network packet ──────────────────────────────────

    /**
     * Invoked by {@code LanguageSyncPacket.handle} when the client reports
     * its language. Stores the code and immediately re-evaluates flight.
     */
    public static void onLanguageReceived(ServerPlayer player, String langCode) {
        if (langCode != null) {
            PLAYER_LANGUAGES.put(player.getUUID(), langCode.toLowerCase());
        }
        updateFlight(player);
    }

    // ── Forge event: equipment change ─────────────────────────────────

    @SubscribeEvent
    public static void onEquipmentChange(final LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Only react to chest-slot changes
        if (event.getSlot() == EquipmentSlot.CHEST) {
            updateFlight(player);
        }
    }

    // ── Forge event: player tick — continuous flight maintenance ──────

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Only process every 20 ticks (1 second) to reduce overhead
        if (player.tickCount % 20 != 0) {
            return;
        }

        updateFlight(player);
    }

    // ── Forge events: login / disconnect cleanup ──────────────────────

    @SubscribeEvent
    public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYER_LANGUAGES.remove(event.getEntity().getUUID());
        // Also revoke flight from the player entity (safety cleanup)
        if (event.getEntity() instanceof ServerPlayer player) {
            revokeFlight(player);
        }
    }

    // ── Core logic ────────────────────────────────────────────────────

    /**
     * Evaluates whether the player should have flight, and enables or
     * disables it accordingly.
     */
    private static void updateFlight(final ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            // 玩家从生存模式（附魔飞行激活）切换到创造/旁观模式时，
            // 需要把飞行速度重置回创造默认值 0.05
            if (ENCHANTMENT_FLYING.remove(player.getUUID())) {
                ((AbilitiesAccessor) player.getAbilities()).everlaartifacts$setFlyingSpeed(0.05F);
                player.onUpdateAbilities();
            }
            return;
        }

        if (shouldHaveFlight(player)) {
            enableFlight(player);
        } else {
            revokeFlight(player);
        }
    }

    /**
     * Returns {@code true} if the player's language is a zh variant AND
     * their chest armour has the Chinese Can Fly enchantment.
     */
    private static boolean shouldHaveFlight(final ServerPlayer player) {
        // Condition 1: zh-variant language
        String lang = PLAYER_LANGUAGES.get(player.getUUID());
        if (lang == null) {
            return false;
        }
        boolean isZh = false;
        for (String prefix : ZH_PREFIXES) {
            if (lang.equals(prefix) || lang.startsWith(prefix + "_")) {
                isZh = true;
                break;
            }
        }
        if (!isZh) {
            return false;
        }

        // Condition 2: enchantment on chest armour
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int level = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.CHINESE_CAN_FLY.get(), chest);
        return level > 0;
    }

    /**
     * Enables creative-style flight at half speed.
     */
    private static void enableFlight(final ServerPlayer player) {
        var abilities = player.getAbilities();
        var a = (AbilitiesAccessor) abilities;
        if (!abilities.mayfly) {
            abilities.mayfly = true;
            a.everlaartifacts$setFlyingSpeed(HALF_CREATIVE_FLY_SPEED);
            player.onUpdateAbilities();
        } else if (a.everlaartifacts$getFlyingSpeed() != HALF_CREATIVE_FLY_SPEED) {
            // Another source enabled flight but with a different speed;
            // only override if it's the default creative speed.
            if (a.everlaartifacts$getFlyingSpeed() == 0.05F) {
                a.everlaartifacts$setFlyingSpeed(HALF_CREATIVE_FLY_SPEED);
                player.onUpdateAbilities();
            }
        }
        ENCHANTMENT_FLYING.add(player.getUUID());
    }

    /**
     * Revokes flight granted by this enchantment.
     */
    private static void revokeFlight(final ServerPlayer player) {
        ENCHANTMENT_FLYING.remove(player.getUUID());
        var abilities = player.getAbilities();
        if (abilities.mayfly && !player.isCreative() && !player.isSpectator()) {
            abilities.mayfly = false;
            abilities.flying = false;
            ((AbilitiesAccessor) abilities).everlaartifacts$setFlyingSpeed(0.05F);
            player.onUpdateAbilities();
        }
    }
}
