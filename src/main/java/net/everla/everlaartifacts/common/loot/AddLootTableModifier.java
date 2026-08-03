package net.everla.everlaartifacts.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * 向已有战利品表追加一个战利品池，不破坏原表内容。
 * 1.20.1 的 Forge 已移除内置的 forge:add_loot 序列化器，故自行实现一个等价的。
 * <p>
 * JSON 字段：
 *   type        — everlaartifacts:add_loot
 *   conditions  — 生效条件（如 forge:loot_table_id 限定目标宝箱）
 *   loot_table  — 要注入的战利品表（本 mod 的 data/everlaartifacts/loot_tables/inject/*）
 */
public class AddLootTableModifier extends LootModifier {

	public static final Codec<AddLootTableModifier> CODEC = RecordCodecBuilder.create(inst ->
		codecStart(inst)
			.and(ResourceLocation.CODEC.fieldOf("loot_table").forGetter(m -> m.lootTable))
			.apply(inst, AddLootTableModifier::new));

	private final ResourceLocation lootTable;

	public AddLootTableModifier(LootItemCondition[] conditions, ResourceLocation lootTable) {
		super(conditions);
		this.lootTable = lootTable;
	}

	@NotNull
	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		// 用原始（raw）生成方法：它只跑 pools/functions，不会调用 ForgeHooks.modifyLoot，
		// 因此注入表的内容不会再走一遍全局战利品修改器，既避免重复注入也避免递归。
		LootTable table = context.getLevel().getServer().getLootData().getLootTable(this.lootTable);
		table.getRandomItemsRaw(context, generatedLoot::add);
		return generatedLoot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
