package net.everla.everlaartifacts.server.handlers.items.two_bread_sandwiched_with_cheese;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

@Mod.EventBusSubscriber
public class TwoBreadSandwichedWithCheeseSoundHandler {
    
    public static void handleTwoBreadSandwichedWithCheeseSound(LevelAccessor world, double x, double y, double z) {
        if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:twobreadsandwichedwithcheese")), SoundSource.PLAYERS, 1, 1);
            } else {
                _level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:twobreadsandwichedwithcheese")), SoundSource.PLAYERS, 1, 1, false);
            }
        }
    }
}