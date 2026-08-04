package net.everla.everlaartifacts.mixin;

import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.everla.everlaartifacts.common.handlers.enchantment.QuickChargeHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quick Charge auto-fire rework.
 * <p>
 * When the {@code enhanceQuickCharge} config option is enabled and a crossbow
 * carries Quick Charge level &gt; 5:
 * <ul>
 *   <li><b>Levels 1–5</b> keep vanilla behaviour (load on release, fire on
 *       next click) — nothing below the threshold is touched.</li>
 *   <li><b>Right-click</b> immediately fires a full-charge volley
 *       (all loaded projectiles at {@code getShootingPower(ItemStack)} = 3.15
 *       for arrows).</li>
 *   <li><b>Holding right-click</b> keeps firing automatically while the player
 *       keeps using the item. Rate starts at 120 rpm at level 6 and gains
 *       +120 rpm per extra level, capped at 720 rpm.</li>
 *   <li><b>Anti-spam</b> — clicks and hold-fire share one rate gate, so the
 *       fire rate stays at the enchantment's rpm no matter how fast the player
 *       clicks; only the first click fires instantly.</li>
 *   <li><b>Fired arrows</b> are tagged so they bypass the target's
 *       invulnerability frames on hit, but each level above 5 reduces their
 *       damage by 12% (max 48% at level 9+) — see
 *       {@link AbstractArrowQuickChargeMixin}.</li>
 * </ul>
 * <p>
 * Implementation notes:
 * <ul>
 *   <li>Firing happens server-side only (the client merely enters the "using"
 *       state so the server keeps the player charging); {@code performShooting}
 *       already no-ops its projectile spawn on the client.</li>
 *   <li>A per-item float accumulator (stored in NBT) tracks the firing rhythm,
 *       so fractional tick intervals (e.g. 720 rpm ≈ 1.67 ticks) average out to
 *       the requested rate.</li>
 *   <li>{@code releaseUsing} is cancelled above level 5 so the vanilla
 *       "load on release" step cannot double-load or fire.</li>
 *   <li>{@code getUseDuration} is pinned to a large constant above level 5 so
 *       the using state never auto-completes while the button is held (vanilla
 *       would compute a negative duration here).</li>
 * </ul>
 *
 * @see EverlaArtifactsConfig#isEnhanceQuickCharge()
 * @see CrossbowItemAccessor
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemQuickChargeMixin {

    /** Ticks per minute (20 tps × 60 s). */
    private static final float TICKS_PER_MINUTE = 1200.0F;

    /** Use duration pinned above level 5 so holding never auto-completes. */
    private static final int PERSISTENT_USE_DURATION = 72000;

    /**
     * Intercepts right-click {@code use}. For Quick Charge &gt; 5 it fires a
     * rate-limited volley (first click fires instantly, later clicks obey the
     * per-level interval), then starts the using state so holding continues
     * firing via {@link #everlaartifacts$autoFireTick}. Below the threshold or
     * with the config off, vanilla behaviour is untouched.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void everlaartifacts$autoFireUse(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!EverlaArtifactsConfig.isEnhanceQuickCharge()) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (everlaartifacts$getQuickChargeLevel(stack) <= 5) {
            return;
        }
        CrossbowItemAccessor accessor = (CrossbowItemAccessor) this;
        if (accessor.everlaartifacts$invokeIsCharged(stack)) {
            // 重入：fireFullVolley 通过 use() 触发发射时的重入调用，交还原版 use() 发射。
            if (QuickChargeHandler.autoFiring) {
                return;
            }
            // 玩家右键一把已装填的 qc>5 弩：统一走 fireFullVolley 发射，
            // 确保箭被标记（其它模组重写的 use()/performShooting() 伤害逻辑也被保留）。
            if (!level.isClientSide()) {
                everlaartifacts$fireFullVolley(level, player, hand, stack);
            }
            cir.setReturnValue(InteractionResultHolder.consume(stack));
            return;
        }

        boolean hasProjectiles = !player.getProjectile(stack).isEmpty();
        if (!level.isClientSide()) {
            everlaartifacts$handleAutoFireUse(level, player, hand, stack);
        }

        if (!hasProjectiles) {
            // Nothing to shoot — mirror vanilla's no-ammo failure.
            cir.setReturnValue(InteractionResultHolder.fail(stack));
            return;
        }

        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(stack, level.isClientSide()));
    }

    /**
     * Per-tick auto-fire while the player holds right-click. Only the server
     * spawns projectiles; the client just keeps the using animation going.
     */
    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void everlaartifacts$autoFireTick(Level level, LivingEntity entity, ItemStack stack, int time,
            CallbackInfo ci) {
        if (level.isClientSide() || !(entity instanceof Player)) {
            return;
        }
        if (!EverlaArtifactsConfig.isEnhanceQuickCharge()) {
            return;
        }
        int quickCharge = everlaartifacts$getQuickChargeLevel(stack);
        if (quickCharge <= 5) {
            return;
        }

        stack.getOrCreateTag().putLong(QuickChargeHandler.AUTO_FIRE_LAST_TICK, level.getGameTime());
        everlaartifacts$rateLimitedFire(level, entity, entity.getUsedItemHand(), stack, 1.0F);
    }

    /**
     * Suppresses the vanilla "load on release" step above level 5 — the
     * auto-fire path in {@code use}/{@code onUseTick} already loads and fires,
     * so release must not also arm the crossbow or double-consume ammo.
     */
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void everlaartifacts$autoFireRelease(ItemStack stack, Level level, LivingEntity entity, int timeLeft,
            CallbackInfo ci) {
        if (EverlaArtifactsConfig.isEnhanceQuickCharge()
                && everlaartifacts$getQuickChargeLevel(stack) > 5) {
            ci.cancel();
        }
    }

    /**
     * Pins the use duration above level 5 so the using state never
     * auto-completes while the button is held (vanilla would compute
     * {@code getChargeDuration + 3}, which is negative for Quick Charge &gt; 5).
     */
    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void everlaartifacts$persistentUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (EverlaArtifactsConfig.isEnhanceQuickCharge()
                && everlaartifacts$getQuickChargeLevel(stack) > 5) {
            cir.setReturnValue(PERSISTENT_USE_DURATION);
        }
    }

    /**
     * Tags every arrow created by a Quick Charge &gt; 5 crossbow so that on hit
     * it bypasses the target's i-frames and deals reduced damage.
     * <p>
     * The reduction factor is stored on the arrow so {@link
     * AbstractArrowQuickChargeMixin} does not need to re-look up the owner's
     * weapon at hit time.
     */
    @Inject(method = "getArrow", at = @At("RETURN"))
    private static void everlaartifacts$tagAutoFireArrow(Level level, LivingEntity entity, ItemStack crossbow,
            ItemStack ammo, CallbackInfoReturnable<AbstractArrow> cir) {
        if (!EverlaArtifactsConfig.isEnhanceQuickCharge()) {
            return;
        }
        int quickCharge = everlaartifacts$getQuickChargeLevel(crossbow);
        if (quickCharge <= 5) {
            return;
        }
        AbstractArrow arrow = cir.getReturnValue();
        if (arrow == null) {
            return;
        }
        float reduction = Math.min(QuickChargeHandler.MAX_DAMAGE_REDUCTION,
                (quickCharge - 5) * QuickChargeHandler.DAMAGE_REDUCTION_PER_LEVEL);
        CompoundTag tag = arrow.getPersistentData();
        tag.putBoolean(QuickChargeHandler.TAG_AUTO_FIRE_ARROW, true);
        tag.putFloat(QuickChargeHandler.TAG_DAMAGE_REDUCTION, reduction);
    }

    /**
     * Rate-limits the shot triggered by a right-click. The first-ever use fires
     * immediately for responsiveness; every later click only fires once the
     * per-level interval has elapsed, so spam-clicking can never out-run the
     * enchantment's rpm. Any click that is absorbed simply keeps the using state
     * going and lets {@link #everlaartifacts$autoFireTick} fire when the gate
     * opens.
     */
    private void everlaartifacts$handleAutoFireUse(Level level, Player player, InteractionHand hand,
            ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        long now = level.getGameTime();
        if (!tag.contains(QuickChargeHandler.AUTO_FIRE_LAST_TICK)) {
            // First use of this crossbow — fire immediately for responsiveness.
            tag.putLong(QuickChargeHandler.AUTO_FIRE_LAST_TICK, now);
            tag.putFloat(QuickChargeHandler.AUTO_FIRE_ACCUM, 0.0F);
            everlaartifacts$fireFullVolley(level, player, hand, stack);
            return;
        }
        // Add the real time that passed while the player was not holding/using
        // (the accumulator only advances during onUseTick), capped at one interval
        // so a long pause yields one immediate shot but never a burst.
        long idle = Math.max(0L, now - tag.getLong(QuickChargeHandler.AUTO_FIRE_LAST_TICK));
        float threshold = TICKS_PER_MINUTE / everlaartifacts$getRpm(everlaartifacts$getQuickChargeLevel(stack));
        tag.putLong(QuickChargeHandler.AUTO_FIRE_LAST_TICK, now);
        everlaartifacts$rateLimitedFire(level, player, hand, stack, Math.min((float) idle, threshold));
    }

    /**
     * Core rate gate shared by clicks and hold-fire: fires only once the
     * accumulator has crossed the per-level interval ({@code 1200 ticks / rpm}),
     * so the effective rate never exceeds the enchantment's rpm no matter how
     * fast the player clicks. The fractional remainder is kept so sub-tick
     * intervals (e.g. 720 rpm ≈ 1.67 ticks) average out to the target rate.
     */
    private void everlaartifacts$rateLimitedFire(Level level, LivingEntity entity, InteractionHand hand,
            ItemStack stack, float elapsed) {
        float threshold = TICKS_PER_MINUTE / everlaartifacts$getRpm(everlaartifacts$getQuickChargeLevel(stack));
        CompoundTag tag = stack.getOrCreateTag();
        float accum = tag.getFloat(QuickChargeHandler.AUTO_FIRE_ACCUM) + elapsed;
        if (accum >= threshold) {
            accum -= threshold;
            everlaartifacts$fireFullVolley(level, entity, hand, stack);
        }
        tag.putFloat(QuickChargeHandler.AUTO_FIRE_ACCUM, accum);
    }

    /**
     * Loads projectiles (if not already charged) and fires one full-charge
     * volley, then clears the charged flag. Mirrors the vanilla charged-crossbow
     * right-click path.
     * <p>
     * Note: {@code tryLoadProjectiles} only populates the {@code ChargedProjectiles}
     * NBT and returns whether ammo was found — it does <b>not</b> set the
     * {@code Charged} flag (vanilla sets it separately in {@code releaseUsing}).
     * The flag is set here so the subsequent {@code use} actually fires.
     * <p>
     * Firing is driven through the crossbow's own {@code use()} instead of calling
     * {@code performShooting()} directly, so overridden {@code use()} /
     * {@code performShooting()} in modded crossbows keep their custom damage
     * rather than falling back to vanilla crossbow damage. The reentrant {@code use}
     * call is absorbed by the "already charged" guard in
     * {@link #everlaartifacts$autoFireUse}.
     */
    private void everlaartifacts$fireFullVolley(Level level, LivingEntity entity, InteractionHand hand,
            ItemStack stack) {
        CrossbowItemAccessor accessor = (CrossbowItemAccessor) this;
        if (!accessor.everlaartifacts$invokeIsCharged(stack)) {
            if (!accessor.everlaartifacts$invokeTryLoadProjectiles(entity, stack)) {
                return; // no ammo to load
            }
            accessor.everlaartifacts$invokeSetCharged(stack, true);
        }
        // 打开自动开火窗口：期间加入世界的箭都会被标记（绕过无敌帧 + 减伤）。
        // 对其它模组自行创建/发射箭的弩同样生效（getArrow 注入覆盖不到它们）。
        QuickChargeHandler.autoFiring = true;
        QuickChargeHandler.autoFiringReduction = Math.min(QuickChargeHandler.MAX_DAMAGE_REDUCTION,
                (everlaartifacts$getQuickChargeLevel(stack) - 5) * QuickChargeHandler.DAMAGE_REDUCTION_PER_LEVEL);
        try {
            if (entity instanceof Player player) {
                ((CrossbowItem) (Object) this).use(level, player, hand);
            } else {
                // 非玩家实体没有 use() 入口，退回直接调用 performShooting()（原逻辑）
                accessor.everlaartifacts$invokePerformShooting(level, entity, hand, stack,
                        accessor.everlaartifacts$invokeGetShootingPower(stack), 1.0F);
            }
        } finally {
            QuickChargeHandler.autoFiring = false;
            QuickChargeHandler.autoFiringReduction = 0.0F;
        }
        accessor.everlaartifacts$invokeSetCharged(stack, false);
    }

    private static int everlaartifacts$getQuickChargeLevel(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
    }

    private static float everlaartifacts$getRpm(int quickChargeLevel) {
        return Math.min(QuickChargeHandler.MAX_RPM,
                (quickChargeLevel - 5) * QuickChargeHandler.RPM_PER_LEVEL);
    }
}
