package net.everla.everlaartifacts.common.handlers.enchantment;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.mixin.LivingEntityLayeredBufferMixin;

/**
 * Handles the Layered Buffer enchantment for absorbing hits.
 * <p>
 * <b>Architecture:</b>
 * <ul>
 *   <li>{@link LivingEntityLayeredBufferMixin} — primary protection at
 *       {@code HEAD} of {@code LivingEntity.hurt()}, the highest possible
 *       priority in the damage pipeline. Runs before any Forge event or
 *       vanilla processing.</li>
 *   <li>{@link #onLivingHurt(LivingHurtEvent)} — secondary safety net
 *       (Forge event). Catches damage that may have been modified and
 *       re-applied after the Mixin interception.</li>
 *   <li>{@link #onLivingDeath(LivingDeathEvent)} — death prevention.
 *       Handled via Forge event because the death sequence spans multiple
 *       vanilla methods.</li>
 * </ul>
 * <p>
 * Shared utility methods ({@link #getDamageLayers}, {@link #setDamageLayers},
 * {@link #calculateThreshold}, {@link #removeEnchantmentAndCleanup}) are
 * {@code public} so the Mixin can call them directly.
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LayeredBufferHandler {

    /** NBT key for the accumulated damage counter on the armour piece. */
    public static final String DAMAGE_KEY = "LayeredBufferDamage";

    private static final String ENCHANTMENTS_KEY = "Enchantments";
    private static final String ID_KEY = "id";

    // ── Primary: Mixin-based interception (see LivingEntityLayeredBufferMixin)
    //    The Mixin injects at HEAD of LivingEntity.hurt() — highest priority.
    //
    // ── Secondary: Forge event handlers below ──────────────────────────

    /**
     * Secondary damage absorption via Forge event.
     * <p>
     * Catches damage that was re-applied or modified after the Mixin
     * interception (e.g. by other mods that cancel and re-dispatch hurt).
     * Uses {@link EventPriority#HIGHEST}.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(final LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack chestArmour = player.getItemBySlot(EquipmentSlot.CHEST);
        int enchantLevel = getEnchantmentLevel(chestArmour);
        if (enchantLevel <= 0 || player.isCreative()) {
            return;
        }

        int currentDamage = getDamageLayers(chestArmour) + 1;
        setDamageLayers(chestArmour, currentDamage);

        if (currentDamage >= calculateThreshold(enchantLevel)) {
            removeEnchantmentAndCleanup(chestArmour, player);
        } else {
            event.setCanceled(true);
            player.invulnerableTime = 0;
        }
    }

    /**
     * Death prevention via Forge event.
     * <p>
     * Cancels death, restores full health, and consumes one buffer layer.
     * The death sequence involves multiple vanilla methods, making a
     * Forge event more practical than multiple Mixin injections.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(final LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack chestArmour = player.getItemBySlot(EquipmentSlot.CHEST);
        int enchantLevel = getEnchantmentLevel(chestArmour);
        if (enchantLevel <= 0 || player.isCreative()) {
            return;
        }

        int currentDamage = getDamageLayers(chestArmour) + 1;
        setDamageLayers(chestArmour, currentDamage);

        if (currentDamage >= calculateThreshold(enchantLevel)) {
            removeEnchantmentAndCleanup(chestArmour, player);
        } else {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.invulnerableTime = 0;

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                spawnDeathPreventionParticles(serverLevel, player);
            }
        }
    }

    // ── Tooltip ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onItemTooltip(final ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        int enchantLevel = getEnchantmentLevel(stack);
        if (enchantLevel <= 0) {
            return;
        }
        int threshold = calculateThreshold(enchantLevel);
        int remaining = threshold - getDamageLayers(stack);

        event.getToolTip().add(Component.translatable(
                "enchantment.everlaartifacts.displaytext.layeredbuffer",
                remaining, threshold)
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    // ── Anvil — reset damage counter on repair ────────────────────────

    @SubscribeEvent
    public static void onAnvilRepair(final AnvilRepairEvent event) {
        ItemStack output = event.getOutput();
        if (output.hasTag() && output.getTag().contains(DAMAGE_KEY)) {
            if (getEnchantmentLevel(output) > 0) {
                setDamageLayers(output, 0);
            } else {
                output.getTag().remove(DAMAGE_KEY);
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(final AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (left.isEmpty()) {
            return;
        }
        int enchantLevel = getEnchantmentLevel(left);
        if (enchantLevel > 0 && left.hasTag() && left.getTag().contains(DAMAGE_KEY)) {
            ItemStack preview = left.copy();
            setDamageLayers(preview, 0);
            event.setOutput(preview);
            event.setCost(1);
        }
    }

    // ── Public utilities (called by Mixin) ────────────────────────────

    /**
     * Reads the accumulated damage counter from the item's NBT.
     */
    public static int getDamageLayers(final ItemStack stack) {
        if (!stack.hasTag()) {
            return 0;
        }
        return stack.getTag().getInt(DAMAGE_KEY);
    }

    /**
     * Writes the accumulated damage counter to the item's NBT.
     */
    public static void setDamageLayers(final ItemStack stack, final int value) {
        stack.getOrCreateTag().putInt(DAMAGE_KEY, value);
    }

    /**
     * Computes the number of hits the buffer can absorb.
     * <p>
     * Formula: {@code k = 40 + log₁₀(level³²)}.
     */
    public static int calculateThreshold(final int level) {
        int clamped = Math.max(level, 1);
        double log10 = Math.log(Math.pow(clamped, 32)) / Math.log(10);
        return (int) Math.round(40 + log10);
    }

    /**
     * Strips the Layered Buffer enchantment from the armour piece and
     * plays break feedback (sound + particles).
     */
    public static void removeEnchantmentAndCleanup(
            final ItemStack stack, final LivingEntity wearer) {

        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            ListTag enchantments = tag.getList(ENCHANTMENTS_KEY, Tag.TAG_COMPOUND);

            ResourceLocation targetId = ForgeRegistries.ENCHANTMENTS.getKey(
                    EverlaartifactsModEnchantments.LAYERED_BUFFER.get());
            if (targetId != null) {
                String targetStr = targetId.toString();
                enchantments.removeIf(enchantmentTag ->
                        targetStr.equals(((CompoundTag) enchantmentTag).getString(ID_KEY)));
                tag.put(ENCHANTMENTS_KEY, enchantments);
            }
            tag.remove(DAMAGE_KEY);
        }

        if (wearer.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null,
                    wearer.getX(), wearer.getY(), wearer.getZ(),
                    SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            double w = wearer.getBbWidth();
            double h = wearer.getBbHeight();
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    wearer.getX(), wearer.getY() + h / 2.0, wearer.getZ(),
                    30, w, h, w, 0.2);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────

    private static int getEnchantmentLevel(final ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.LAYERED_BUFFER.get(), stack);
    }

    private static void spawnDeathPreventionParticles(
            final ServerLevel level, final Player player) {
        double w = player.getBbWidth();
        double h = player.getBbHeight();
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + h / 2.0, player.getZ(),
                30, w, h, w, 0.1);
    }
}
