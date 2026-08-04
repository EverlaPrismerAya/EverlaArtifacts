package net.everla.everlaartifacts.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@link CrossbowItem}'s {@code private static} helpers so the
 * Quick Charge auto-fire mixin can drive a full-charge shot.
 * <p>
 * All five target methods are {@code private static} in vanilla, which a
 * subclass mixin cannot call directly — the codebase's preferred way to reach
 * them is an {@code @Invoker} accessor interface.
 *
 * @see CrossbowItemQuickChargeMixin
 */
@Mixin(CrossbowItem.class)
public interface CrossbowItemAccessor {

    /** @see CrossbowItem#isCharged(ItemStack) */
    @Invoker("isCharged")
    boolean everlaartifacts$invokeIsCharged(ItemStack stack);

    /** @see CrossbowItem#setCharged(ItemStack, boolean) */
    @Invoker("setCharged")
    void everlaartifacts$invokeSetCharged(ItemStack stack, boolean charged);

    /** @see CrossbowItem#tryLoadProjectiles(LivingEntity, ItemStack) */
    @Invoker("tryLoadProjectiles")
    boolean everlaartifacts$invokeTryLoadProjectiles(LivingEntity entity, ItemStack stack);

    /** @see CrossbowItem#performShooting(Level, LivingEntity, InteractionHand, ItemStack, float, float) */
    @Invoker("performShooting")
    void everlaartifacts$invokePerformShooting(Level level, LivingEntity entity, InteractionHand hand,
            ItemStack stack, float velocity, float inaccuracy);

    /** @see CrossbowItem#getShootingPower(ItemStack) */
    @Invoker("getShootingPower")
    float everlaartifacts$invokeGetShootingPower(ItemStack stack);
}
