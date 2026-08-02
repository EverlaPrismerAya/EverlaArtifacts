package net.everla.everlaartifacts.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin that exposes the protected {@code getPickupItem()} method
 * on {@link AbstractArrow} so that {@link LivingEntityImpalingMixin} can
 * read enchantments from thrown trident projectiles.
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Invoker("getPickupItem")
    ItemStack everlaartifacts$invokeGetPickupItem();
}
