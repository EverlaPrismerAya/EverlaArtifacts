package net.everla.everlaartifacts.server.handlers.items.braised_pork_with_plum_cabbage;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

@Mod.EventBusSubscriber
public class TakeoffAtAPowerOfFiveWattsHandler {
    
    public static void handleTakeoffAtAPowerOfFiveWatts(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null)
            return;
        if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:five_watt")), SoundSource.NEUTRAL, 1, 1);
            } else {
                _level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:five_watt")), SoundSource.NEUTRAL, 1, 1, false);
            }
        }
        entity.push(0, 4, 0);
    }
}