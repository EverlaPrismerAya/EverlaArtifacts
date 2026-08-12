package net.everla.everlaartifacts.init;

import net.everla.everlaartifacts.common.advancements.SonicBoomWardenKillTrigger;
import net.minecraft.advancements.CriteriaTriggers;

/**
 * 自定义进度触发器注册。
 * <p>
 * 1.20.1 的触发器不是注册表条目，而是 {@link CriteriaTriggers} 里的静态 map；
 * 用 {@link CriteriaTriggers#register} 在模组构造期注册，进度的
 * {@code trigger} 字段按 {@code <modid>:<name>} 引用。
 */
public class EverlaartifactsModTriggerTypes {

	public static void register() {
		CriteriaTriggers.register(SonicBoomWardenKillTrigger.INSTANCE);
	}
}
