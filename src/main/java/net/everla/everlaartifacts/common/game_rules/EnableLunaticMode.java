package net.everla.everlaartifacts.common.game_rules;

import net.minecraft.world.level.GameRules;

public class EnableLunaticMode {
    public static final GameRules.Key<GameRules.BooleanValue> ENABLE_LUNATIC_MODE =
            GameRules.register("enableLunaticMode", GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
}