package net.everla.everlaartifacts;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.config.EverlaArtifactsConfig;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LayeredBufferHandler {
    
    // 损伤NBT标签键
    private static final String LAYERED_BUFFER_DAMAGE_KEY = "LayeredBufferDamage";
    
    // 用于跟踪玩家上次的生命值
    private static final Map<UUID, Float> lastHealthMap = new HashMap<>();
    
    // 检查配置中指定的模组是否已加载或是否强制启用
    private static boolean isFullProtectionEnabled() {
        // 检查是否强制启用LayeredBuffer完整保护
        if (EverlaArtifactsConfig.isForceEnableLayeredBuffer()) {
            return true;
        }
        
        // 否则检查配置中指定的模组是否已加载
        String modIdsString = EverlaArtifactsConfig.getFullProtectionModIds();
        if (modIdsString != null && !modIdsString.trim().isEmpty()) {
            // 将逗号分隔的字符串转换为数组
            String[] modIds = modIdsString.split(",");
            for (String modId : modIds) {
                String trimmedModId = modId.trim();
                if (!trimmedModId.isEmpty() && ModList.get().isLoaded(trimmedModId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // 检查并处理Layered Buffer附魔逻辑
        if (processLayeredBufferLogic(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST))) {
            // 无条件取消伤害事件，玩家不会受到任何伤害
            event.setCanceled(true);
            
            // 移除玩家的受击无敌帧
            player.invulnerableTime = 0;
        }
    }
    
    // 仅在配置的模组加载时启用的事件
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!isFullProtectionEnabled()) {
            return; // 如果配置的模组未加载，则跳过此事件处理
        }
        
        LivingEntity entity = event.getEntity();
        
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // 检查玩家是否穿戴了带有Layered Buffer附魔的护甲
        ItemStack chestArmor = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.LAYERED_BUFFER.get(), chestArmor);
        
        if (enchantmentLevel > 0) {
            // 检查实体是否为无敌状态（创造模式玩家）
            if (player.isCreative()) {
                // 如果是创造模式玩家，则不叠加损伤
                return;
            }
            
            // 增加损伤层数
            int currentDamage = getDamageLayers(chestArmor);
            currentDamage++;
            setDamageLayers(chestArmor, currentDamage);
            
            // 计算阈值 k = 40 + log₁₀(x³²)，其中 x 是附魔等级
            int threshold = calculateThreshold(enchantmentLevel);
            
            // 如果损伤达到阈值，移除附魔
            if (currentDamage >= threshold) {
                // 移除附魔并清理相关数据
                removeLayeredBufferEnchantmentAndCleanup(chestArmor, player);
            } else {
                // 显示当前损伤状态的粒子效果
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        player.getX(),
                        player.getY() + player.getBbHeight() / 2.0,
                        player.getZ(),
                        10, // 增加粒子数量
                        player.getBbWidth() / 2.0, // x轴扩散范围
                        player.getBbHeight() / 2.0, // y轴扩散范围
                        player.getBbWidth() / 2.0, // z轴扩散范围
                        0.1 // 粒子速度
                    );
                }
            }
            
            // 无条件取消攻击事件，防止伤害发生
            event.setCanceled(true);
            
            // 移除玩家的受击无敌帧
            player.invulnerableTime = 0;
        }
    }
    
    // 仅在配置的模组加载时启用的事件
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!isFullProtectionEnabled()) {
            return; // 如果配置的模组未加载，则跳过此事件处理
        }
        
        LivingEntity entity = event.getEntity();
        
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // 关键：检查玩家是否穿戴了带有Layered Buffer附魔的护甲
        // 不管是什么原因导致的死亡，只要穿戴了该附魔的护甲就取消死亡
        ItemStack chestArmor = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        
        // 检查并处理Layered Buffer附魔逻辑
        if (processLayeredBufferLogic(player, chestArmor)) {
            // 取消死亡事件，恢复玩家生命值
            event.setCanceled(true);
            
            // 恢复玩家生命值到满血
            player.setHealth(player.getMaxHealth());
            
            // 移除玩家的受击无敌帧
            player.invulnerableTime = 0;
            
            // 播放保护音效
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
                );
                
                // 在玩家周围产生保护粒子效果
                serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX(),
                    player.getY() + player.getBbHeight() / 2.0,
                    player.getZ(),
                    30, // 增加粒子数量
                    player.getBbWidth(), // x轴扩散范围
                    player.getBbHeight(), // y轴扩散范围
                    player.getBbWidth(), // z轴扩散范围
                    0.1 // 粒子速度
                );
            }
        }
    }
    
    // 仅在配置的模组加载时启用的事件
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!isFullProtectionEnabled()) {
            return; // 如果配置的模组未加载，则跳过此事件处理
        }
        
        LivingEntity entity = event.getEntity();
        
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // 检查并处理Layered Buffer附魔逻辑
        if (processLayeredBufferLogic(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST))) {
            // 设置伤害为0
            event.setAmount(0.0F);
            
            // 重置玩家的受伤和死亡动画时间
            player.hurtTime = 0;
            player.deathTime = 0;
            
            // 移除玩家的受击无敌帧
            player.invulnerableTime = 0;
        }
    }
    
    // 仅在配置的模组加载时启用的事件
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!isFullProtectionEnabled()) {
            return; // 如果配置的模组未加载，则跳过此事件处理
        }
        
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        float currentHealth = player.getHealth();
        
        // 检查生命值是否突然大幅下降，这可能是被Infinity Sword攻击的迹象
        if (lastHealthMap.containsKey(playerId)) {
            float lastHealth = lastHealthMap.get(playerId);
            
            // 如果生命值突然大幅下降（比如减少了超过一半或接近死亡）
            if (lastHealth > currentHealth && (lastHealth - currentHealth) > lastHealth * 0.7f) {
                // 检查并处理Layered Buffer附魔逻辑
                if (processLayeredBufferLogic(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST))) {
                    // 恢复玩家生命值
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
        
        // 更新最后的生命值
        lastHealthMap.put(playerId, currentHealth);
    }
    
    // 仅在配置的模组加载时启用的事件
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!isFullProtectionEnabled()) {
            return; // 如果配置的模组未加载，则跳过此事件处理
        }
        
        Player player = event.getEntity();
        // 清除玩家的生命值缓存
        lastHealthMap.remove(player.getUUID());
    }
    
    // 仅在配置的模组加载时启用的事件
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!isFullProtectionEnabled()) {
            return; // 如果配置的模组未加载，则跳过此事件处理
        }
        
        Player player = event.getEntity();
        // 初始化玩家的生命值缓存
        lastHealthMap.put(player.getUUID(), player.getHealth());
    }
    
    // 仅在配置的模组加载时启用的事件
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!isFullProtectionEnabled()) {
            return; // 如果配置的模组未加载，则跳过此事件处理
        }
        
        Player player = event.getEntity();
        // 清除玩家的生命值缓存
        lastHealthMap.remove(player.getUUID());
    }
    
    // 为带有LayeredBuffer附魔的物品添加工具提示，显示损伤值
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        
        if (stack.isEmpty()) {
            return;
        }
        
        // 检查物品是否有LayeredBuffer附魔
        int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.LAYERED_BUFFER.get(), stack);
        
        if (enchantmentLevel > 0) {

            
            // 计算阈值
            int threshold = calculateThreshold(enchantmentLevel);

            // 获取损伤值
            int damage = threshold - getDamageLayers(stack);
            
            // 添加损伤信息到工具提示，使用本地化键名
            event.getToolTip().add(Component.translatable("enchantment.everlaartifacts.displaytext.layeredbuffer", damage, threshold)
                .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        }
    }
    
    private static int getDamageLayers(ItemStack stack) {
        if (!stack.hasTag()) {
            return 0;
        }
        
        return stack.getTag().getInt(LAYERED_BUFFER_DAMAGE_KEY);
    }
    
    private static void setDamageLayers(ItemStack stack, int damage) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }
        
        stack.getTag().putInt(LAYERED_BUFFER_DAMAGE_KEY, damage);
    }
    
    private static int calculateThreshold(int level) {
        // k = 40 + log₁₀(level³²)
        // 使用换底公式: log₁₀(x) = ln(x) / ln(10)
        if (level < 1) level = 1; // 确保底数 >= 1
        
        double logValue = Math.log(Math.pow(level, 32)) / Math.log(10);
        return (int) Math.round(40 + logValue);
    }
    
    private static void removeLayeredBufferEnchantmentAndCleanup(ItemStack stack, LivingEntity entity) {
        if (!stack.hasTag()) {
            // 即使没有标签也清除损伤数据
            return;
        }
        
        // 从物品上移除附魔
        ListTag enchantments = stack.getTag().getList("Enchantments", Tag.TAG_COMPOUND);
        ResourceLocation enchantmentIdToRemove = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(EverlaartifactsModEnchantments.LAYERED_BUFFER.get());
        String enchantmentIdString = enchantmentIdToRemove.toString();
        
        for (int i = 0; i < enchantments.size(); ) {
            CompoundTag enchantment = enchantments.getCompound(i);
            String id = enchantment.getString("id");
            
            if (id.equals(enchantmentIdString)) {
                enchantments.remove(i);
                break;
            } else {
                i++;
            }
        }
        
        stack.getTag().put("Enchantments", enchantments);
        
        // 播放音效
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, 
                entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.BEACON_DEACTIVATE, 
                SoundSource.PLAYERS,
                1.0F, 
                1.0F
            );
            
            // 在穿戴者四周产生粒子效果
            serverLevel.sendParticles(
                ParticleTypes.ENCHANTED_HIT,
                entity.getX(),
                entity.getY() + entity.getBbHeight() / 2.0,
                entity.getZ(),
                30, // 增加粒子数量
                entity.getBbWidth(), // x轴扩散范围
                entity.getBbHeight(), // y轴扩散范围
                entity.getBbWidth(), // z轴扩散范围
                0.2 // 粒子速度
            );
        }
        
        // 清除损伤相关的NBT数据
        if (stack.hasTag()) {
            stack.getTag().remove(LAYERED_BUFFER_DAMAGE_KEY);
        }
    }
    // 在铁砧实际修复物品时清理损伤值
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack repairedItem = event.getOutput();
        
        // 检查修理后的物品是否有LayeredBuffer附魔
        int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.LAYERED_BUFFER.get(), repairedItem);
        
        // 无论修理后是否保留附魔，只要进行了修理操作就清理损伤数据
        if (enchantmentLevel > 0 && repairedItem.hasTag() && repairedItem.getTag().contains(LAYERED_BUFFER_DAMAGE_KEY)) {
            // 当物品仍带有附魔时，重置损伤值为0
            setDamageLayers(repairedItem, 0);
        } else if (enchantmentLevel == 0 && repairedItem.hasTag() && repairedItem.getTag().contains(LAYERED_BUFFER_DAMAGE_KEY)) {
            // 如果修理后不再有附魔，则完全移除损伤数据
            CompoundTag tag = repairedItem.getTag();
            if (tag != null) {
                // 仅移除损伤相关的NBT标签，保留其他所有标签
                tag.remove(LAYERED_BUFFER_DAMAGE_KEY);
            }
        }
    }

    // 在铁砧预览更新时清理损伤值
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack leftItem = event.getLeft();
        
        if (leftItem.isEmpty()) {
            return;
        }
        
        // 检查物品是否有LayeredBuffer附魔
        int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.LAYERED_BUFFER.get(), leftItem);
        
        // 如果物品带有附魔且有损伤数据，在预览中将其损伤值重置为0
        if (enchantmentLevel > 0 && leftItem.hasTag() && leftItem.getTag().contains(LAYERED_BUFFER_DAMAGE_KEY)) {
            // 创建一个副本进行预览，不直接修改原物品
            ItemStack previewItem = leftItem.copy();
            setDamageLayers(previewItem, 0);
            event.setOutput(previewItem);
            event.setCost(1); // 设置修复费用
        }
    }
    
    /**
     * 统一处理Layered Buffer附魔的核心逻辑
     * @param player 玩家实体
     * @param armor 装备的护甲
     * @return 是否成功处理了附魔逻辑
     */
    private static boolean processLayeredBufferLogic(Player player, ItemStack armor) {
        // 检查护甲是否带有Layered Buffer附魔
        int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.LAYERED_BUFFER.get(), armor);
        
        if (enchantmentLevel <= 0) {
            return false;
        }
        
        // 检查实体是否为无敌状态（创造模式玩家）
        if (player.isCreative()) {
            // 如果是创造模式玩家，则不叠加损伤
            return false;
        }
        
        // 增加损伤层数
        int currentDamage = getDamageLayers(armor);
        currentDamage++;
        setDamageLayers(armor, currentDamage);
        
        // 计算阈值 k = 40 + log₁₀(x³²)，其中 x 是附魔等级
        int threshold = calculateThreshold(enchantmentLevel);
        
        // 如果损伤达到阈值，移除附魔
        if (currentDamage >= threshold) {
            // 移除附魔并清理相关数据
            removeLayeredBufferEnchantmentAndCleanup(armor, player);
            return false; // 附魔已移除，返回false表示不应用保护
        } else {
            // 显示当前损伤状态的粒子效果
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    player.getX(),
                    player.getY() + player.getBbHeight() / 2.0,
                    player.getZ(),
                    10, // 增加粒子数量
                    player.getBbWidth() / 2.0, // x轴扩散范围
                    player.getBbHeight() / 2.0, // y轴扩散范围
                    player.getBbWidth() / 2.0, // z轴扩散范围
                    0.1 // 粒子速度
                );
            }
            return true; // 成功应用了保护
        }
    }
}