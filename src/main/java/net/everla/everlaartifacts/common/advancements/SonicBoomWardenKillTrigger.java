package net.everla.everlaartifacts.common.advancements;

import com.google.gson.JsonObject;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 自定义进度触发器：用音爆（Sonic Boom）附魔击败监守者。
 * <p>
 * 由 {@code SonicBoomHandler} 在监守者死于玩家音爆时调用 {@link #fire(ServerPlayer)}，
 * 用于解锁「呀卡吗洗！」目标级进度。
 */
public class SonicBoomWardenKillTrigger
		extends SimpleCriterionTrigger<SonicBoomWardenKillTrigger.TriggerInstance> {

	public static final ResourceLocation ID = new ResourceLocation(EverlaartifactsMod.MODID, "sonic_boom_warden_kill");

	/** 单例：注册进 {@code trigger_type} 注册表的就是这个实例，触发用的也是它。 */
	public static final SonicBoomWardenKillTrigger INSTANCE = new SonicBoomWardenKillTrigger();

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
			DeserializationContext context) {
		return new TriggerInstance(player);
	}

	/** 触发本进度的条件：玩家用音爆附魔击杀了一只监守者。 */
	public static void fire(ServerPlayer player) {
		INSTANCE.trigger(player, instance -> true);
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {

		public TriggerInstance(ContextAwarePredicate player) {
			super(SonicBoomWardenKillTrigger.ID, player);
		}

		public static TriggerInstance wardenKilledWithSonicBoom() {
			return new TriggerInstance(ContextAwarePredicate.ANY);
		}
	}
}
