package net.everla.everlaartifacts.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin that exposes protected methods on {@link LivingEntity}
 * for use by mod items that need to trigger vanilla loot / drop logic.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    /**
     * Exposes the protected {@code dropFromLootTable(DamageSource, boolean)}
     * method so that {@code ProcedureSwordItem} can trigger loot drops
     * without fragile reflection.
     *
     * @param damageSource the damage source that caused the death
     * @param recentlyHit  whether the entity was recently hit by a player
     *                     (affects looting enchantment behavior)
     */
    @Invoker("dropFromLootTable")
    void everlaartifacts$invokeDropFromLootTable(
            DamageSource damageSource, boolean recentlyHit);
}
