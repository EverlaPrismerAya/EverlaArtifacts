package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.minecraft.tags.TagKey;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LunaticDamageHandler {
    
    private static final TagKey<EntityType<?>> BOSSES_TAG = 
        TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
    
    // 禁用真伤的Boss实体ID列表（从配置获取）
    private static java.util.Set<String> getDisabledTrueDamageBosses() {
        return EverlaArtifactsConfig.getDisabledTrueDamageBossesSet();
    }
    
    /**
     * 检查攻击是否来自Boss实体（包括直接攻击和投掷物）
     */
    private static boolean isBossAttack(Entity attacker) {
        if (attacker == null) {
            return false;
        }
        
        // 直接攻击：攻击者本身就是Boss
        if (attacker instanceof LivingEntity livingAttacker && 
            livingAttacker.getType().is(BOSSES_TAG)) {
            // 检查是否为需要排除的Boss
            String entityId = EntityType.getKey(livingAttacker.getType()).toString();
            return !getDisabledTrueDamageBosses().contains(entityId);
        }
        
        // 投掷物攻击：检查投掷物的拥有者是否为Boss
        if (attacker instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity livingOwner && 
                livingOwner.getType().is(BOSSES_TAG)) {
                // 检查是否为需要排除的Boss
                String entityId = EntityType.getKey(livingOwner.getType()).toString();
                return !getDisabledTrueDamageBosses().contains(entityId);
            }
        }
        
        return false;
    }
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // 检查是否启用了月狂模式且世界难度为困难
        if (player.level().getDifficulty() != Difficulty.HARD) {
            return;
        }
        
        GameRules gameRules = player.level().getGameRules();
        if (!gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE)) {
            return;
        }
        
        DamageSource damageSource = event.getSource();
        Entity attacker = damageSource.getEntity();
        
        // 检查是否为Boss实体的攻击（包括直接攻击和投掷物）
        if (isBossAttack(attacker)) {
            // 检查玩家当前生命值
            if (player.getHealth() <= 1.0f) {
                // 玩家生命值小于等于1时，使用命运秒杀
                // 创建基于数据包的destiny_kill伤害源
                Holder<DamageType> damageType = player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, 
                        ResourceLocation.fromNamespaceAndPath("everlaartifacts", "destiny_kill")))
                    .orElseThrow();
                DamageSource destinySource = new DamageSource(damageType);
                player.hurt(destinySource, Integer.MAX_VALUE);
            } else {
                // Boss攻击额外通过setHealth减少玩家生命值5%
                float healthReduction = player.getMaxHealth() * 0.05f;
                float newHealth = Math.max(0, player.getHealth() - healthReduction);
                player.setHealth(newHealth);
            }
        }
        
        // 检查是否为生物攻击类型
        if (damageSource.is(DamageTypes.MOB_ATTACK)) {
            // mob_attack伤害增加20%
            float increasedDamage = event.getAmount() * 1.2f;
            event.setAmount(increasedDamage);
        }
    }
}