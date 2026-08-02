package net.everla.everlaartifacts.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin into {@link ItemStack#getTooltipLines} to reorder the tooltip so that
 * mod-added entries (from {@code ItemTooltipEvent}) appear <b>before</b> the
 * F3+H advanced tooltip section (durability, item ID, NBT tag count).
 * <p>
 * Uses two cooperating injection points:
 * <ol>
 *   <li><b>Pre-advanced inject</b> — captures the precise list index just
 *       before the advanced section via {@code @Local} capture. This provides
 *       the accurate insertion point without heuristics.</li>
 *   <li><b>Redirect on the Forge event</b> — fires the event normally, then
 *       moves newly-added entries before the captured index. If the local
 *       capture failed (e.g. due to another mod's mixin altering the local
 *       variable table), falls back to a heuristic search on the pre-event
 *       (clean) tooltip list.</li>
 * </ol>
 * <b>Compatibility:</b> uses {@link Redirect} rather than {@code RETURN}
 * injection to avoid collisions. The {@code @Local} capture uses
 * {@link LocalCapture#CAPTURE_FAILSOFT} — if it fails, the heuristic
 * fallback still produces correct results for standard tooltip layouts.
 */
@Mixin(ItemStack.class)
public abstract class ItemTooltipOrderMixin {

    /** Cached DARK_GRAY TextColor for the heuristic fallback. */
    private static final TextColor DARK_GRAY_COLOR =
            TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY);

    /**
     * ThreadLocal carrying the precise advanced-section start index from the
     * pre-advanced inject to the redirect handler. {@code null} means the
     * capture failed and the heuristic fallback should be used.
     */
    @Unique
    private static final ThreadLocal<Integer> preciseAdvancedStart =
            new ThreadLocal<>();

    // ────────────────────────────────────────────────────────────────
    // Injection 1 — capture the advanced section start index
    // ────────────────────────────────────────────────────────────────

    /**
     * Injects just before the second {@code TooltipFlag.isAdvanced()} check
     * (bytecode offset ~1234), which guards the durability / item ID / NBT
     * count block. Captures the tooltip list size at this point — this is
     * exactly where the advanced section begins.
     */
    @Inject(method = "getTooltipLines",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/TooltipFlag;"
                           + "isAdvanced()Z",
                    ordinal = 1),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private void everlaartifacts$captureAdvancedStart(
            Player player, TooltipFlag flag, CallbackInfo ci,
            List<Component> tooltip) {
        // The tooltip list is captured via @Local (the only List<Component>
        // local at this point). On failure, tooltip will be null and the
        // redirect handler will use the heuristic fallback.
        if (tooltip != null) {
            preciseAdvancedStart.set(tooltip.size());
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Injection 2 — intercept the event, reorder entries
    // ────────────────────────────────────────────────────────────────

    /**
     * Redirects the {@link ForgeEventFactory#onItemTooltip} call.
     * <p>
     * Pre-event: locates the advanced section (using the precise index from
     * injection 1, or the heuristic fallback).
     * <p>
     * Post-event: moves any entries appended during the event to before the
     * advanced section, so mod tooltips appear above the item ID line.
     */
    @Redirect(method = "getTooltipLines",
            at = @At(value = "INVOKE",
                    remap = false,
                    target = "Lnet/minecraftforge/event/ForgeEventFactory;"
                           + "onItemTooltip(Lnet/minecraft/world/item/ItemStack;"
                           + "Lnet/minecraft/world/entity/player/Player;"
                           + "Ljava/util/List;"
                           + "Lnet/minecraft/world/item/TooltipFlag;)"
                           + "Lnet/minecraftforge/event/entity/player/"
                           + "ItemTooltipEvent;"))
    private ItemTooltipEvent everlaartifacts$reorderTooltips(
            ItemStack stack, Player player, List<Component> tooltip,
            TooltipFlag flag) {

        // ── Pre-event: capture state, then locate the advanced section ──
        Integer preciseStart = preciseAdvancedStart.get();
        preciseAdvancedStart.remove(); // clear for next call

        int sizeBeforeEvent = tooltip.size();

        int advancedStart;
        int advancedEnd;

        if (preciseStart != null && preciseStart >= 0 && preciseStart < tooltip.size()) {
            // Precise capture succeeded — advancedStart is the exact index
            // where durability / ID / NBT begin. Everything from there to
            // sizeBeforeEvent-1 is the advanced section (plus any entries
            // other mods' mixins may have inserted).
            advancedStart = preciseStart;
            advancedEnd = sizeBeforeEvent - 1;
        } else if (flag.isAdvanced()) {
            // Fallback: heuristic search on the clean (pre-event) list.
            // Since no mod entries exist yet, false positives are unlikely.
            int idIndex = findItemIdLine(tooltip);
            if (idIndex >= 0) {
                advancedStart = findAdvancedStart(tooltip, idIndex);
                advancedEnd = findAdvancedEnd(tooltip, idIndex);
            } else {
                advancedStart = -1;
                advancedEnd = -1;
            }
        } else {
            advancedStart = -1;
            advancedEnd = -1;
        }

        // ── Fire the event (mods add their tooltips here) ──
        ItemTooltipEvent event = ForgeEventFactory.onItemTooltip(
                stack, player, tooltip, flag);

        // ── Post-event: move mod entries before the advanced section ──
        if (advancedStart >= 0 && advancedEnd >= advancedStart
                && tooltip.size() > sizeBeforeEvent) {
            List<Component> modEntries = new ArrayList<>(
                    tooltip.subList(sizeBeforeEvent, tooltip.size()));
            tooltip.subList(sizeBeforeEvent, tooltip.size()).clear();

            // advancedStart may have shifted if entries were also removed,
            // but we only remove from the tail (after advancedEnd), so
            // advancedStart is still correct.
            // Clamp to current list bounds for safety.
            int insertAt = Math.min(advancedStart, tooltip.size());
            tooltip.addAll(insertAt, modEntries);
        }

        return event;
    }

    // ────────────────────────────────────────────────────────────────
    // Heuristic helpers (fallback only)
    // ────────────────────────────────────────────────────────────────

    /**
     * Finds the F3+H item ID line in a <b>clean</b> (pre-event) tooltip.
     * <p>
     * The ID is always {@code Component.literal("namespace:path")} with
     * {@code ChatFormatting.DARK_GRAY}. This is hardcoded in vanilla and
     * cannot be altered by resource packs.
     */
    private static int findItemIdLine(List<Component> tooltip) {
        for (int i = 2; i < tooltip.size(); i++) {
            Component comp = tooltip.get(i);
            if (DARK_GRAY_COLOR.equals(comp.getStyle().getColor())) {
                String text = comp.getString();
                if (text.indexOf(':') > 0 && !text.contains(" ")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findAdvancedStart(List<Component> tooltip, int idIndex) {
        if (idIndex > 0 && isDurabilityLine(tooltip.get(idIndex - 1).getString())) {
            return idIndex - 1;
        }
        return idIndex;
    }

    private static int findAdvancedEnd(List<Component> tooltip, int idIndex) {
        int end = idIndex;
        if (idIndex + 1 < tooltip.size()) {
            Component next = tooltip.get(idIndex + 1);
            if (DARK_GRAY_COLOR.equals(next.getStyle().getColor())) {
                String text = next.getString();
                if (text.contains("NBT") || text.contains("tag")) {
                    end = idIndex + 1;
                }
            }
        }
        if (end + 1 < tooltip.size()) {
            String text = tooltip.get(end + 1).getString();
            if (text.contains("disabled") || text.contains("DISABLED")) {
                end = end + 1;
            }
        }
        return end;
    }

    private static boolean isDurabilityLine(String text) {
        int slashIdx = text.indexOf('/');
        if (slashIdx <= 0 || slashIdx >= text.length() - 1) {
            return false;
        }
        boolean hasDigitBefore = false;
        for (int i = 0; i < slashIdx; i++) {
            if (Character.isDigit(text.charAt(i))) {
                hasDigitBefore = true;
                break;
            }
        }
        boolean hasDigitAfter = false;
        for (int i = slashIdx + 1; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                hasDigitAfter = true;
                break;
            }
        }
        return hasDigitBefore && hasDigitAfter;
    }
}
