package net.everla.everlaartifacts.server.handlers.enchantment;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class LiveWireHandler {

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }

        // 检查主手武器是否有LiveWire附魔
        ItemStack mainHand = player.getMainHandItem();
        int liveWireLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.LIVE_WIRE.get(), mainHand);

        // 只有附魔等级大于0时才触发效果
        if (liveWireLevel <= 0) {
            return;
        }

        // 取消原始攻击事件
        event.setCanceled(true);

        // 获取玩家的基础攻击力
        float baseAttackDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        
        // 获取目标的护甲值
        float targetArmor = (float) target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        
        // 计算闪电伤害：基础攻击力 + 附魔等级 * 敌方护甲值 / 10
        float lightningDamage = baseAttackDamage + (liveWireLevel * targetArmor / 10.0f);

        // 应用攻击冷却影响（使用攻击强度比例）
        float attackStrength = player.getAttackStrengthScale(0.5f);
        lightningDamage *= attackStrength;
        
        // 检查是否触发下落暴击（需要攻击冷却进度≥90%）
        boolean isCriticalHit = player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && 
            !player.isInWater() && !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && 
            !player.isPassenger() && attackStrength >= 0.9F;
            
        // 应用下落暴击加成
        if (isCriticalHit) {
            // 下落暴击倍率：1.5倍基础伤害
            lightningDamage *= 1.5F;
        }
        
        // 造成主要目标闪电伤害（视为玩家攻击，确保经验掉落）
        boolean damageSuccess = target.hurt(level.damageSources().playerAttack(player), lightningDamage);
        
        // Homa Staff兼容：如果玩家持有Homa Staff且具有Homa Active效果且伤害成功，则附加Blood Blossom效果
        if (damageSuccess && mainHand.getItem() == EverlaartifactsModItems.HOMA_STAFF.get() && 
            player.hasEffect(EverlaartifactsModMobEffects.HOMA_ACTIVE.get())) {
            target.addEffect(new MobEffectInstance(
                EverlaartifactsModMobEffects.BLOOD_BLOSSOM.get(),
                200, // 持续时间 10秒 (200 ticks)
                0    // 等级 0
            ));
        }
        
        // 检查是否具有横扫之刃附魔
        int sweepingEdgeLevel = EnchantmentHelper.getItemEnchantmentLevel(
            net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE, mainHand);
        
        // 如果有横扫之刃，对附近实体造成溅射伤害
        if (sweepingEdgeLevel > 0) {
            applySweepingDamage(player, target, lightningDamage, sweepingEdgeLevel, level, mainHand);
        }

        // 只有攻击冷却满时才生成视觉闪电
        if (attackStrength >= 1.0f) {
            // 在目标位置生成仅视觉的闪电
            spawnVisualLightning((ServerLevel) level, target.getX(), target.getY(), target.getZ());
        }
        
        // 如果触发暴击，生成灵魂火粒子并播放暴击音效
        if (isCriticalHit && level instanceof ServerLevel serverLevel) {
            // 生成灵魂火粒子效果
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), 
                target.getY() + target.getBbHeight() * 0.5, 
                target.getZ(),
                15, // 粒子数量
                0.3, // x轴扩散
                0.3, // y轴扩散
                0.3, // z轴扩散
                0.05 // 粒子速度
            );
            
            // 播放原版暴击音效
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // 播放音效：Homa Staff使用专用音效，其他情况使用普通音效
        ResourceLocation soundEvent = mainHand.getItem() == EverlaartifactsModItems.HOMA_STAFF.get() ?
            ResourceLocation.fromNamespaceAndPath("everlaartifacts", "live_wire_homa_staff") :
            ResourceLocation.fromNamespaceAndPath("everlaartifacts", "live_wire");
            
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
            ForgeRegistries.SOUND_EVENTS.getValue(soundEvent),
            SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 应用横扫之刃溅射伤害
     */
    private static void applySweepingDamage(Player player, LivingEntity target, float baseDamage, int sweepingLevel, Level level, ItemStack mainHand) {
        // 获取附近1.5方块范围内的实体
        java.util.List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            target.getBoundingBox().inflate(1.5),
            entity -> entity != player && entity != target && entity.isAlive()
        );
        
        // 计算溅射伤害：0.1倍原伤害 × 横扫之刃等级
        float splashDamage = baseDamage * 0.1f * sweepingLevel;
        
        // 对每个附近实体造成溅射伤害
        for (LivingEntity nearbyEntity : nearbyEntities) {
            boolean splashDamageSuccess = nearbyEntity.hurt(level.damageSources().playerAttack(player), splashDamage);
            
            // Homa Staff兼容：如果玩家持有Homa Staff且具有Homa Active效果且溅射伤害成功，则附加Blood Blossom效果
            if (splashDamageSuccess && mainHand.getItem() == EverlaartifactsModItems.HOMA_STAFF.get() && 
                player.hasEffect(EverlaartifactsModMobEffects.HOMA_ACTIVE.get())) {
                nearbyEntity.addEffect(new MobEffectInstance(
                    EverlaartifactsModMobEffects.BLOOD_BLOSSOM.get(),
                    165, // 持续时间 8.25秒
                    0    // 等级 0
                ));
            }
        }
        
        // 生成横扫粒子效果
        if (!nearbyEntities.isEmpty() && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                target.getX(),
                target.getY() + 0.5,
                target.getZ(),
                1,
                0.0, 0.0, 0.0,
                0.0
            );
        }
    }
    
    /**
     * 生成仅视觉的闪电效果
     */
    private static void spawnVisualLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(level);
        if (entityToSpawn != null) {
            entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y, z)));
            entityToSpawn.setVisualOnly(true);
            level.addFreshEntity(entityToSpawn);
        }
    }
}