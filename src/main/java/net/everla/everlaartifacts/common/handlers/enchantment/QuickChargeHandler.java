package net.everla.everlaartifacts.common.handlers.enchantment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shared constants for the Quick Charge &gt; 5 auto-fire rework, plus the
 * world-level arrow tagging that makes the i-frame bypass work for modded
 * crossbows too.
 * <p>
 * Deliberately a plain (non-mixin) class: Mixin forbids non-private static
 * fields inside a {@code @Mixin} class, so the NBT keys, tuning values, and the
 * auto-fire window flags that both {@code CrossbowItemQuickChargeMixin} and
 * {@code AbstractArrowQuickChargeMixin} need are kept here and referenced
 * instead. The class also subscribes to {@link EntityJoinLevelEvent} so arrows
 * spawned by <b>modded</b> crossbows (which may not go through
 * {@code CrossbowItem.getArrow()}) still get tagged while the auto-fire window
 * is open.
 *
 * @see net.everla.everlaartifacts.mixin.CrossbowItemQuickChargeMixin
 * @see net.everla.everlaartifacts.mixin.AbstractArrowQuickChargeMixin
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public final class QuickChargeHandler {

    /** NBT key for the auto-fire accumulator on the crossbow stack. */
    public static final String AUTO_FIRE_ACCUM = "everlaartifacts:auto_fire_accum";

    /** NBT key for the game time the auto-fire rhythm was last advanced. */
    public static final String AUTO_FIRE_LAST_TICK = "everlaartifacts:auto_fire_last_tick";

    /** NBT key marking arrows fired by a Quick Charge >5 crossbow. */
    public static final String TAG_AUTO_FIRE_ARROW = "everlaartifacts:auto_fire_arrow";

    /** NBT key storing the damage-reduction factor on such arrows. */
    public static final String TAG_DAMAGE_REDUCTION = "everlaartifacts:auto_fire_damage_reduction";

    /** Damage reduction gained per Quick Charge level above 5. */
    public static final float DAMAGE_REDUCTION_PER_LEVEL = 0.04F;

    /** Maximum damage reduction (reached at Quick Charge 9). */
    public static final float MAX_DAMAGE_REDUCTION = 0.4F;

    /** RPM gained per Quick Charge level above 5. */
    public static final float RPM_PER_LEVEL = 120.0F;

    /** Absolute cap on the auto-fire rate. */
    public static final float MAX_RPM = 1200.0F;

    /**
     * True while the auto-fire mixin is inside a crossbow's {@code use()}, i.e.
     * any arrow currently being added to the world belongs to that volley.
     * Only ever set on the server thread; the window spans a single synchronous
     * {@code addFreshEntity} chain, so a plain boolean is safe.
     */
    public static boolean autoFiring = false;

    /** Damage-reduction factor to stamp on arrows spawned while {@link #autoFiring}. */
    public static float autoFiringReduction = 0.0F;

    /**
     * Tags every arrow that joins the world while the auto-fire window is open.
     * The {@code getArrow} mixin inject only covers arrows created through
     * {@code CrossbowItem.getArrow()}; modded crossbows that create/fire their
     * projectiles another way are caught here instead, so their arrows also
     * bypass the target's i-frames and get the per-level damage reduction.
     */
    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !autoFiring) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        CompoundTag tag = arrow.getPersistentData();
        tag.putBoolean(TAG_AUTO_FIRE_ARROW, true);
        tag.putFloat(TAG_DAMAGE_REDUCTION, autoFiringReduction);
    }

    private QuickChargeHandler() {
    }
}
