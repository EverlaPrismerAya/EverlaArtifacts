package net.everla.everlaartifacts.common.handlers.items.deepseek;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Curios API 集成：为深度求索之戒附加 Curios 的 ITEM 能力，使其可作为戒指佩戴。
 * <p>
 * Curios 是可选依赖，对 Curios 类的引用都只在 {@link #isCuriosLoaded()} 为真时执行
 * （JVM 对方法体引用的类惰性解析）。未加载时该物品保持普通物品，由
 * {@code DeepSeekHandler} 的副手判定兜底。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeepSeekCuriosHandler {

    private static boolean curiosLoaded = false;
    private static boolean curiosChecked = false;

    /** 懒加载并缓存 Curios 是否已加载，避免在 ModList 初始化前被触发 */
    private static boolean isCuriosLoaded() {
        if (!curiosChecked) {
            curiosChecked = true;
            try {
                ModList modList = ModList.get();
                curiosLoaded = modList != null && modList.isLoaded("curios");
            } catch (Exception e) {
                curiosLoaded = false;
            }
        }
        return curiosLoaded;
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        if (!isCuriosLoaded()) {
            return;
        }
        ItemStack stack = event.getObject();
        if (stack.getItem() != EverlaartifactsModItems.DEEPSEEK.get()) {
            return;
        }
        event.addCapability(new ResourceLocation(EverlaartifactsMod.MODID, "curio"),
                new CurioProvider(stack));
    }

    /** 为该物品堆提供 Curios 的 ITEM 能力 */
    private static class CurioProvider implements ICapabilityProvider {
        private final ItemStack stack;

        CurioProvider(ItemStack stack) {
            this.stack = stack;
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == CuriosCapability.ITEM) {
                return LazyOptional.of(() -> (ICurio) new DeepSeekCurio(stack)).cast();
            }
            return LazyOptional.empty();
        }
    }

    /** 最小的 ICurio 实现：默认行为即可，仅需返回物品堆 */
    private static class DeepSeekCurio implements ICurio {
        private final ItemStack stack;

        DeepSeekCurio(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getStack() {
            return stack;
        }
    }
}
