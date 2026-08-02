package net.everla.everlaartifacts.mixin;

import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code flyingSpeed} field on {@link Abilities}
 * for the Chinese Can Fly enchantment's flight-speed control.
 */
@Mixin(Abilities.class)
public interface AbilitiesAccessor {

    @Accessor("flyingSpeed")
    void everlaartifacts$setFlyingSpeed(float speed);

    @Accessor("flyingSpeed")
    float everlaartifacts$getFlyingSpeed();
}
