package net.everla.everlaartifacts.server.handlers.items.venus_shell;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.item.VenusShellItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VenusShellHandler {
    

    
    /**
     * 被动效果：每tick检查玩家是否持有Venus Shell，如果是则给予被动状态效果
     * 状态效果视为信标给予，会自动显示粒子效果
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        
        // 检查玩家是否持有Venus Shell
        if (isHoldingVenusShell(player)) {
            // 给予5刻(0.25秒)的被动效果，这样每tick都会刷新
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                EverlaartifactsModMobEffects.VENUS_SHELL_PASSIVE.get(), 
                5, 
                0, 
                true,
                true
            ));
        }
    }
    
    /**
     * 主动效果：处理右击事件，给予主动状态效果并设置冷却
     * 状态效果视为信标给予，会自动显示粒子效果
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        
        // 只在服务端处理
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return;
        }
        
        // 检查是否是Venus Shell
        if (stack.getItem() instanceof VenusShellItem) {
            // 给予500刻(25秒)的主动效果
            serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                EverlaartifactsModMobEffects.VENUS_SHELL_ACTIVE.get(),
                500,
                0,
                true,
                true
            ));
            
            // 对周围实体造成基于护甲值的魔法伤害
            applyVenusShellDamage(serverPlayer);
            
            // 设置原版物品冷却（200刻/10秒）
            player.getCooldowns().addCooldown(stack.getItem(), 200);
        }
    }
    
    /**
     * 检查玩家是否持有Venus Shell
     */
    private static boolean isHoldingVenusShell(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        return mainHand.getItem() instanceof VenusShellItem || 
               offHand.getItem() instanceof VenusShellItem;
    }
    

    
    /**
     * 对周围实体造成基于护甲值的魔法伤害
     * 半径2方块内，排除玩家自身
     */
    private static void applyVenusShellDamage(ServerPlayer player) {
        // 获取玩家当前位置
        var pos = player.position();
        var level = player.level();
        
        // 获取玩家护甲值，如果没有护甲值则按1处理
        var armorAttribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        double armorValue = armorAttribute != null ? Math.max(armorAttribute.getValue(), 1.0) : 1.0;
        
        // 计算伤害值：105.0%护甲值
        double damage = armorValue * 1.05;
        
        // 查找半径2方块内的实体
        var entities = level.getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            player.getBoundingBox().inflate(2.0),
            entity -> entity != player && entity.isAlive()
        );
        
        // 对每个实体造成魔法伤害
        for (var entity : entities) {
            // 使用间接魔法伤害源，标识攻击者为玩家
            var damageSource = player.damageSources().indirectMagic(player, player);
            
            // 绕过无敌帧
            int originalInvulnerableTime = entity.invulnerableTime;
            entity.invulnerableTime = 0;
            
            // 造成伤害
            entity.hurt(damageSource, (float) damage);
            
            // 恢复无敌帧时间
            entity.invulnerableTime = originalInvulnerableTime;
        }
    }
}