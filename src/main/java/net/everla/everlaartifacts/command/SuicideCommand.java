package net.everla.everlaartifacts.command;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.common.util.FakePlayerFactory;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.Commands;

import net.everla.everlaartifacts.server.handlers.commands.EverlaKillHandler;

@Mod.EventBusSubscriber
public class SuicideCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("suicide")

				.executes(arguments -> {
					Level world = arguments.getSource().getUnsidedLevel();
					double x = arguments.getSource().getPosition().x();
					double y = arguments.getSource().getPosition().y();
					double z = arguments.getSource().getPosition().z();
					Entity entity = arguments.getSource().getEntity();
					if (entity == null && world instanceof ServerLevel _servLevel)
						entity = FakePlayerFactory.getMinecraft(_servLevel);
					Direction direction = Direction.DOWN;
					if (entity != null)
						direction = entity.getDirection();

					// 直接实现自杀逻辑，不依赖Procedure
					if (entity != null && entity instanceof Player player && !player.level().isClientSide()) {
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
					return 0;
				}));
	}
}