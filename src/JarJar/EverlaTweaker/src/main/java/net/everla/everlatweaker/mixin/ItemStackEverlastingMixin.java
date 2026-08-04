package net.everla.everlatweaker.mixin;

import net.everla.everlatweaker.common.handlers.data_driven.EverlastingItemHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Mixin to cancel durability consumption for items with the
 * {@code everlatweaker:everlasting} tag.
 * <p>
 * Instead of setting the {@code Unbreakable:1b} NBT tag after-the-fact,
 * this intercepts {@link ItemStack#hurtAndBreak} at the source and
 * prevents any durability loss from occurring. The item never takes
 * damage, so it doesn't need to carry an NBT flag.
 * <p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackEverlastingMixin {

    /**
     * Cancels {@code hurtAndBreak} for items tagged as everlasting.
     * This is the central method for all durability consumption in Minecraft —
     * tools, weapons, armor, and shields all go through this path.
     */
    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true)
    private <T extends LivingEntity> void everlaartifacts$cancelHurtAndBreak(
            int amount, T entity, Consumer<T> onBroken, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (EverlastingItemHandler.isEverlasting(self)) {
            ci.cancel();
        }
    }
}
