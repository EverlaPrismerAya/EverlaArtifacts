package net.everla.everlaartifacts.server.handlers.enchantment;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Handles the attack speed bonus for the "N卡网速快"
 * (Nvidia Network Quality) enchantment.
 * <p>
 * Applies an {@link AttributeModifier} to the player's
 * {@link Attributes#ATTACK_SPEED} when the main-hand weapon carries the
 * enchantment. The modifier is updated or removed when the held item changes.
 * <p>
 * Formula: +20% at level 1, +5% per additional level, capped at +50%.
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NvidiaNetworkQualityHandler {

    /** Fixed UUID so the modifier can be reliably removed. */
    private static final UUID MODIFIER_UUID =
            UUID.fromString("f3a2b1c0-1d2e-3f4a-5b6c-7d8e9f0a1b2c");

    private static final String MODIFIER_NAME = "nvidia_network_quality_speed";

    /** Maximum attack speed bonus as a multiplier (50% → 0.5). */
    private static final float MAX_BONUS = 0.5F;

    /** Base bonus at level 1 (20% → 0.2). */
    private static final float BASE_BONUS = 0.2F;

    /** Bonus per additional level (5% → 0.05). */
    private static final float PER_LEVEL_BONUS = 0.05F;

    // ── Equipment change ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onEquipmentChange(final LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSlot().isArmor()) {
            return; // only care about hand slots
        }
        updateModifier(player);
    }

    // ── Player tick ───────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        // Check every 20 ticks (1 second) as a safety net
        if (player.tickCount % 20 != 0) {
            return;
        }
        updateModifier(player);
    }

    // ── Cleanup on disconnect / respawn ───────────────────────────────

    @SubscribeEvent
    public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeModifier(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeModifier(player);
        }
    }

    // ── Core logic ────────────────────────────────────────────────────

    private static void updateModifier(final ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) {
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        int level = EnchantmentHelper.getTagEnchantmentLevel(
                EverlaartifactsModEnchantments.NVIDIA_NETWORK_QUALITY.get(), weapon);

        if (level <= 0 || weapon.isEmpty()) {
            removeModifier(player);
            return;
        }

        // Compute bonus with cap
        float bonus = BASE_BONUS + (level - 1) * PER_LEVEL_BONUS;
        if (bonus > MAX_BONUS) {
            bonus = MAX_BONUS;
        }

        // Remove old modifier if present (different level → different value)
        attr.removeModifier(MODIFIER_UUID);

        attr.addTransientModifier(new AttributeModifier(
                MODIFIER_UUID, MODIFIER_NAME, bonus,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void removeModifier(final ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.removeModifier(MODIFIER_UUID);
        }
    }
}
