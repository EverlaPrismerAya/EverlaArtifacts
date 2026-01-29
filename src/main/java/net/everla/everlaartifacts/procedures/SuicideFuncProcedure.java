package net.everla.everlaartifacts.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.everla.everlaartifacts.EverlaKillHandler;

public class SuicideFuncProcedure {
    public static void execute(Entity entity) {
        if (entity == null || !(entity instanceof Player player) || player.level().isClientSide())
            return;

        Component deathMessage = Component.translatable(
            "text.everlaartifacts.suicide",
            player.getDisplayName()
        );

        EverlaKillHandler.killPlayer(
            player,
            "everlaartifacts:suicide",
            deathMessage,
            ResourceLocation.tryParse("everlaartifacts:deltarune_explosion")
        );
    }
}