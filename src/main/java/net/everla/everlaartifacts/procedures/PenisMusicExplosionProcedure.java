package net.everla.everlaartifacts.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.everla.everlaartifacts.EverlaKillHandler;
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class PenisMusicExplosionProcedure {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != event.getEntity().getUsedItemHand())
            return;
        execute(event, event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        execute(null, world, x, y, z, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null || !(entity instanceof Player player) || world.isClientSide())
            return;

        if (world.getBlockState(BlockPos.containing(x, y, z)).getBlock() != Blocks.JUKEBOX)
            return;

        ItemStack heldItem = player.getMainHandItem();
        ResourceLocation tagLoc = ResourceLocation.tryParse("everlatweaker:penis_music");
        if (tagLoc == null || !heldItem.is(TagKey.create(Registries.ITEM, tagLoc)))
            return;

        if (Math.random() > 0.1)
            return;

        // 特有视觉效果（保留在调用前）
        player.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.WAAOOO_OVERLAY.get(), 10, 1));

        Component deathMessage = Component.translatable(
            "text.everlaartifacts.penis_music_explosion",
            player.getDisplayName()
        );

        EverlaKillHandler.killPlayer(
            player,
            "everlaartifacts:penis_music_explosion",
            deathMessage,
            ResourceLocation.tryParse("everlaartifacts:deltarune_explosion")
        );
    }
}