package net.everla.everlaartifacts.common.handlers.enchantment;

/**
 * Shared constants for the Quick Charge &gt; 5 auto-fire rework.
 * <p>
 * Deliberately a plain (non-mixin) class: Mixin forbids non-private static
 * fields inside a {@code @Mixin} class, so the NBT keys and tuning values that
 * both {@code CrossbowItemQuickChargeMixin} and {@code AbstractArrowQuickChargeMixin}
 * need are kept here and referenced instead.
 *
 * @see net.everla.everlaartifacts.mixin.CrossbowItemQuickChargeMixin
 * @see net.everla.everlaartifacts.mixin.AbstractArrowQuickChargeMixin
 */
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
    public static final float DAMAGE_REDUCTION_PER_LEVEL = 0.12F;

    /** Maximum damage reduction (reached at Quick Charge 9). */
    public static final float MAX_DAMAGE_REDUCTION = 0.48F;

    /** RPM gained per Quick Charge level above 5. */
    public static final float RPM_PER_LEVEL = 120.0F;

    /** Absolute cap on the auto-fire rate. */
    public static final float MAX_RPM = 720.0F;

    private QuickChargeHandler() {
    }
}
