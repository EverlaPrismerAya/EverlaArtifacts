
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.everla.everlaartifacts.EverlaartifactsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EverlaartifactsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EverlaartifactsMod.MODID);
	public static final RegistryObject<CreativeModeTab> EVERLA_TWEAKER = REGISTRY.register("everla_tweaker",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.everlaartifacts.everla_tweaker")).icon(() -> new ItemStack(EverlaartifactsModItems.THREE_INTERWINED_FATE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(EverlaartifactsModBlocks.DEEPSLATE_AURIC_ORE.get().asItem());
				tabData.accept(EverlaartifactsModItems.RAW_AURIC.get());
				tabData.accept(EverlaartifactsModItems.AURIC_SCRAP.get());
				tabData.accept(EverlaartifactsModItems.AURIC_SCRAP_BLOCK.get());
				tabData.accept(EverlaartifactsModItems.AURIC_INGOT.get());
				tabData.accept(EverlaartifactsModItems.DRAGON_SOUL_FRAGMENT.get());
				tabData.accept(EverlaartifactsModItems.WITHER_ESSENCE.get());
				tabData.accept(EverlaartifactsModItems.RED_PACKET.get());
				tabData.accept(EverlaartifactsModItems.THREE_INTERWINED_FATE.get());
				tabData.accept(EverlaartifactsModItems.FIRECRACKER.get());
				tabData.accept(EverlaartifactsModItems.HOMA_STAFF.get());
				tabData.accept(EverlaartifactsModItems.VENUS_SHELL.get());
				tabData.accept(EverlaartifactsModItems.BRACKETS_BLADE.get());
				tabData.accept(EverlaartifactsModItems.PROCEDURE_SWORD.get());
				tabData.accept(EverlaartifactsModItems.GIGABYTE_MEMORY_RING.get());
				tabData.accept(EverlaartifactsModItems.ATM_RING.get());
				tabData.accept(EverlaartifactsModItems.DEEPSEEK.get());
				tabData.accept(EverlaartifactsModItems.GLASSES.get());
				tabData.accept(EverlaartifactsModItems.GAMING_CATTLE.get());
			}).build());
	public static final RegistryObject<CreativeModeTab> WEIRD_THING = REGISTRY.register("weird_thing",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.everlaartifacts.weird_thing")).icon(() -> new ItemStack(EverlaartifactsModItems.BRAISED_PORK_WITH_PLUM_CABBAGE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(EverlaartifactsModItems.ZAKO_UNCLE.get());
				tabData.accept(EverlaartifactsModItems.POT_OF_PAIN.get());
				tabData.accept(EverlaartifactsModItems.CHINESE_DUMPLING.get());
				tabData.accept(EverlaartifactsModItems.TWO_BREAD_SANDWICHED_WITH_CHEESE.get());
				tabData.accept(EverlaartifactsModItems.WEIRD_COCKTAIL.get());
				tabData.accept(EverlaartifactsModItems.BRAISED_PORK_WITH_PLUM_CABBAGE.get());
				tabData.accept(EverlaartifactsModItems.NILK.get());
				tabData.accept(EverlaartifactsModItems.WEIRD_FISH_STEW.get());
				tabData.accept(EverlaartifactsModItems.PAY_TO_WIN_SHARD_1.get());
				tabData.accept(EverlaartifactsModItems.PAY_TO_WIN_SHARD_2.get());
				tabData.accept(EverlaartifactsModItems.PAY_TO_WIN_SHARD_3.get());
				tabData.accept(EverlaartifactsModItems.PAY_TO_WIN_SHARD_4.get());
				tabData.accept(EverlaartifactsModItems.PAY_TO_WIN_SHARD_5.get());
				tabData.accept(EverlaartifactsModItems.PAY_TO_WIN_CRYSTAL.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_1.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_2.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_3.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_4.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_5.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_6.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_7.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_8.get());
				tabData.accept(EverlaartifactsModItems.CONDENCED_POTATO_9.get());
				tabData.accept(EverlaartifactsModItems.CHALICE_OF_BLOOD_GOD.get());
				tabData.accept(EverlaartifactsModItems.NUCLEAR_WASTE_WATER_BUCKET.get());
				tabData.accept(EverlaartifactsModItems.INNER_QUARTZ_OUTER_NUCLEAR.get());
				tabData.accept(EverlaartifactsModItems.BEIJING_TICKET.get());
				tabData.accept(EverlaartifactsModItems.NANJING_TICKET.get());
				tabData.accept(EverlaartifactsModItems.TOKYO_TICKET.get());
			}).build());
	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(EverlaartifactsModItems.FIRECRACKER.get());
			tabData.accept(EverlaartifactsModItems.HOMA_STAFF.get());
			tabData.accept(EverlaartifactsModItems.VENUS_SHELL.get());
			tabData.accept(EverlaartifactsModItems.BRACKETS_BLADE.get());
			tabData.accept(EverlaartifactsModItems.PROCEDURE_SWORD.get());
		}
	}
}
