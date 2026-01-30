package net.everla.everlaartifacts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class OverlayTestCommand {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("overlaytest")
            .then(Commands.argument("offset_from_bottom", IntegerArgumentType.integer(0)) // 从底部的偏移量
                .executes(context -> {
                    int offsetFromBottom = IntegerArgumentType.getInteger(context, "offset_from_bottom");
                    
                    // 触发覆盖显示（使用从底部的偏移量）
                    net.everla.everlaartifacts.OverlayTestRenderer.showOverlay(offsetFromBottom);
                    
                    return 1;
                })
            )
        );
    }
}