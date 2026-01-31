package net.everla.everlaartifacts.game_rules;

import net.minecraft.world.level.GameRules;

public class ForceUseTruePerformance {
    public static final GameRules.Key<GameRules.BooleanValue> FORCE_USE_TRUE_PERFORMANCE =
            GameRules.register("ForceUseTruePerformance", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
}