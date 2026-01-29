
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
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
				tabData.accept(EverlaartifactsModBlocks.AURIC_SCRAP_BLOCK.get().asItem());
				tabData.accept(EverlaartifactsModItems.AURIC_INGOT.get());
				tabData.accept(EverlaartifactsModItems.RED_PACKET.get());
				tabData.accept(EverlaartifactsModItems.NUCLEAR_WASTE_WATER_BUCKET.get());
				tabData.accept(EverlaartifactsModItems.INNER_QUARTZ_OUTER_NUCLEAR.get());
				tabData.accept(EverlaartifactsModItems.BEIJING_TICKET.get());
				tabData.accept(EverlaartifactsModItems.NANJING_TICKET.get());
				tabData.accept(EverlaartifactsModItems.TOKYO_TICKET.get());
			}).build());
	public static final RegistryObject<CreativeModeTab> WEIRD_THING = REGISTRY.register("weird_thing",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.everlaartifacts.weird_thing")).icon(() -> new ItemStack(EverlaartifactsModItems.BRAISED_PORK_WITH_PLUM_CABBAGE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(EverlaartifactsModItems.ZAKO_UNCLE.get());
				tabData.accept(EverlaartifactsModItems.POT_OF_PAIN.get());
				tabData.accept(EverlaartifactsModItems.CHINESE_DUMPLING.get());
				tabData.accept(EverlaartifactsModItems.TWO_BREAD_SANDWICHED_WITH_CHEESE.get());
				tabData.accept(EverlaartifactsModItems.THREE_INTERWINED_FATE.get());
				tabData.accept(EverlaartifactsModItems.WEIRD_COCKTAIL.get());
				tabData.accept(EverlaartifactsModItems.BRAISED_PORK_WITH_PLUM_CABBAGE.get());
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
				tabData.accept(EverlaartifactsModItems.NILK.get());
			}).withTabsBefore(EVERLA_TWEAKER.getId()).build());
	public static final RegistryObject<CreativeModeTab> EVERLA_DISCS = REGISTRY.register("everla_discs",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.everlaartifacts.everla_discs")).icon(() -> new ItemStack(EverlaartifactsModItems.TWISTED_GARDEN.get())).displayItems((parameters, tabData) -> {
				tabData.accept(EverlaartifactsModItems.GALATIC_DESTRUCTION.get());
				tabData.accept(EverlaartifactsModItems.GLUTINOUS_ARBITRATION.get());
				tabData.accept(EverlaartifactsModItems.VISCOUS_DESPERATION.get());
				tabData.accept(EverlaartifactsModItems.TWISTED_GARDEN.get());
				tabData.accept(EverlaartifactsModItems.THOUSAND_LOVE.get());
				tabData.accept(EverlaartifactsModItems.I_REALLY_WANT_TO_STAY_AT_YOUR_HOUSE.get());
				tabData.accept(EverlaartifactsModItems.KILL_THE_MESSENGER.get());
				tabData.accept(EverlaartifactsModItems.ALEPH_0.get());
				tabData.accept(EverlaartifactsModItems.CRUEL_ANGEL.get());
				tabData.accept(EverlaartifactsModItems.DELICATE_WEAPON.get());
				tabData.accept(EverlaartifactsModItems.SWEET_DEATH.get());
				tabData.accept(EverlaartifactsModItems.NEVER_GONNA_GIVE_YOU_UP.get());
				tabData.accept(EverlaartifactsModItems.HARDEST_2_BE.get());
				tabData.accept(EverlaartifactsModItems.REAL_SMOKER.get());
				tabData.accept(EverlaartifactsModItems.HACKER_GAMER.get());
				tabData.accept(EverlaartifactsModItems.FADING_SKY.get());
				tabData.accept(EverlaartifactsModItems.ELECTRICAL_STICK_BONE.get());
				tabData.accept(EverlaartifactsModItems.LONELY_GUITAR.get());
				tabData.accept(EverlaartifactsModItems.THEONLYTHINGIKNOWFORREAL.get());
				tabData.accept(EverlaartifactsModItems.GIRLGOTOLOVE.get());
				tabData.accept(EverlaartifactsModItems.YAMATO.get());
				tabData.accept(EverlaartifactsModItems.NANOMACHINE.get());
				tabData.accept(EverlaartifactsModItems.RAIDEN.get());
				tabData.accept(EverlaartifactsModItems.WORST_APPLE.get());
				tabData.accept(EverlaartifactsModItems.TOKYO_HOT_DISC.get());
				tabData.accept(EverlaartifactsModItems.MANBO_LEGEND.get());
				tabData.accept(EverlaartifactsModItems.MANBO_NO_MORE.get());
				tabData.accept(EverlaartifactsModItems.LONELY_MAN_BROKENHEARTED_SONG.get());
				tabData.accept(EverlaartifactsModItems.MUSIC_DISC_NILK.get());
			}).withTabsBefore(WEIRD_THING.getId()).build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(EverlaartifactsModItems.FIRECRACKER.get());
			tabData.accept(EverlaartifactsModItems.HOMA_STAFF.get());
		}
	}
}
