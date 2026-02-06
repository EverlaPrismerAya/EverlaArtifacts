package net.everla.everlaartifacts.server.handlers.items.enchanted_apple;

import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.registries.ForgeRegistries;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;

import javax.annotation.Nullable;
import java.util.WeakHashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class BadAppleSoundHandler {
    private static final Map<Entity, Long> lastTriggerMap = new WeakHashMap<>();

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event != null && event.getEntity() != null) {
            execute(event, event.getEntity(), event.getItem());
        }
    }

    public static void execute(Entity entity, ItemStack itemstack) {
        execute(null, entity, itemstack);
    }

    private static void execute(@Nullable Event event, Entity entity, ItemStack itemstack) {
        if (entity == null || entity.level().isClientSide())
            return;
        long currentTime = System.currentTimeMillis();
        Long lastTrigger = lastTriggerMap.get(entity);
        if (lastTrigger != null && currentTime - lastTrigger < 1000) { // 至少间隔1秒
            return;
        }

        if (itemstack.is(ItemTags.create(new ResourceLocation("everlatweaker:bad_apple")))) {
            if (Math.random() < 0.05) {
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:xeloc_bad_apple")),
                        SoundSource.PLAYERS, 0.7F, 1.0F);
                }
                if (entity instanceof Player player) {
                    ItemStack worstApple = new ItemStack(EverlaartifactsModItems.WORST_APPLE.get());
                    worstApple.setCount(1);
                    ItemHandlerHelper.giveItemToPlayer(player, worstApple);
                }
                lastTriggerMap.put(entity, currentTime);
            }
        }
    }
}