package net.everla.everlaartifacts.server.handlers.items.red_packet;

import net.everla.everlaartifacts.config.EverlaArtifactsConfig;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.time.LocalDate;
import java.time.Month;

@Mod.EventBusSubscriber
public class RedPacketHandler {
    
    /**
     * 打开红包的处理方法（原RedPacketOpenProcedure的逻辑）
     */
    public static void openRedPacket(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
        if (entity == null)
            return;
        if (!(new Object() {
            public boolean checkGamemode(Entity _ent) {
                if (_ent instanceof net.minecraft.server.level.ServerPlayer _serverPlayer) {
                    return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
                } else if (_ent.level().isClientSide() && _ent instanceof Player _player) {
                    // 修复客户端侧的创造性模式检测
                    net.minecraft.client.multiplayer.ClientPacketListener connection = 
                        net.minecraft.client.Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        net.minecraft.client.multiplayer.PlayerInfo playerInfo = 
                            connection.getPlayerInfo(_player.getGameProfile().getId());
                        return playerInfo != null && playerInfo.getGameMode() == GameType.CREATIVE;
                    }
                    return false;
                }
                return false;
            }
        }.checkGamemode(entity))) {
            itemstack.shrink(1);
        }
        
        if (world instanceof ServerLevel _level) {
            // 生成基础奖励 - 1个金锭
            ItemEntity baseGoldIngot = new ItemEntity(_level, x, y, z, new ItemStack(Items.GOLD_INGOT));
            baseGoldIngot.setPickUpDelay(10);
            _level.addFreshEntity(baseGoldIngot);
            
            // 随机生成额外的金锭 (0-6个)
            int extraGoldIngots = getRandomCount(0.5, 6);
            for (int i = 0; i < extraGoldIngots; i++) {
                ItemEntity goldIngot = new ItemEntity(_level, x, y, z, new ItemStack(Items.GOLD_INGOT));
                goldIngot.setPickUpDelay(10);
                _level.addFreshEntity(goldIngot);
            }
            
            // 生成基础奖励 - 1个钻石
            ItemEntity baseDiamond = new ItemEntity(_level, x, y, z, new ItemStack(Items.DIAMOND));
            baseDiamond.setPickUpDelay(10);
            _level.addFreshEntity(baseDiamond);
            
            // 随机生成额外的钻石 (0-5个)
            int extraDiamonds = getRandomCount(0.5, 5);
            for (int i = 0; i < extraDiamonds; i++) {
                ItemEntity diamond = new ItemEntity(_level, x, y, z, new ItemStack(Items.DIAMOND));
                diamond.setPickUpDelay(10);
                _level.addFreshEntity(diamond);
            }
            
            // 生成钻石块 (10%概率)
            if (Math.random() < 0.1) {
                ItemEntity diamondBlock = new ItemEntity(_level, x, y, z, new ItemStack(Blocks.DIAMOND_BLOCK));
                diamondBlock.setPickUpDelay(10);
                _level.addFreshEntity(diamondBlock);
            }
            
            // 生成下界合金锭 (1%概率)
            if (Math.random() < 0.01) {
                ItemEntity netheriteIngot = new ItemEntity(_level, x, y, z, new ItemStack(Items.NETHERITE_INGOT));
                netheriteIngot.setPickUpDelay(10);
                _level.addFreshEntity(netheriteIngot);
            }
        }
    }
    
    /**
     * 根据概率和最大数量随机生成物品数量
     * @param probability 单次生成概率
     * @param maxCount 最大生成次数
     * @return 实际生成数量
     */
    private static int getRandomCount(double probability, int maxCount) {
        int count = 0;
        for (int i = 0; i < maxCount; i++) {
            if (Math.random() < probability) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 检查当前日期是否为新年期间（1月1日）
     */
    private static boolean isNewYearPeriod() {
        LocalDate now = LocalDate.now();
        return (now.getMonth() == Month.JANUARY && now.getDayOfMonth() == 1);
    }
    
    /**
     * 检查当前日期是否为圣诞节期间（根据配置）
     */
    private static boolean isChristmasPeriod() {
        LocalDate now = LocalDate.now();
        int currentDayOfYear = now.getMonthValue() * 100 + now.getDayOfMonth();
        int startDate = EverlaArtifactsConfig.getRedPacketChristmasStartDate();
        int endDate = EverlaArtifactsConfig.getRedPacketChristmasEndDate();
        
        // 如果结束日期小于开始日期，说明跨年了（例如1224-1231）
        if (endDate >= startDate) {
            return currentDayOfYear >= startDate && currentDayOfYear <= endDate;
        } else {
            // 跨年的特殊情况，如从12月24日到次年1月3日
            // 这种情况其实不会发生，因为圣诞期间通常都在同一年内
            // 但如果配置为跨年（如1224到103），则按以下逻辑处理
            if (now.getMonthValue() >= (startDate / 100)) { // 当前月份大于等于开始月份
                return currentDayOfYear >= startDate;
            } else { // 当前月份小于开始月份（如1月3日，配置为1224-103）
                return currentDayOfYear <= endDate;
            }
        }
    }
    
    /**
     * 监听生物死亡事件，随机掉落红包
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        LevelAccessor level = entity.level();
        
        // 只在服务端执行
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // 检查是否是玩家杀死的生物（非玩家击杀不掉落）
            if (event.getSource().getEntity() instanceof Player) {
                // 检查是否是普通生物（排除玩家）
                if (!(entity instanceof Player)) {
                    double dropChance = 0.0;
                    
                    if (isNewYearPeriod()) {
                        dropChance = EverlaArtifactsConfig.getRedPacketDropChanceNewYear();
                    } else if (isChristmasPeriod()) {
                        dropChance = EverlaArtifactsConfig.getRedPacketDropChanceChristmas();
                    }
                    
                    // 按概率掉落红包
                    if (Math.random() < dropChance) {
                        ItemStack redPacketStack = new ItemStack(EverlaartifactsModItems.RED_PACKET.get());
                        ItemEntity itemEntity = new ItemEntity(serverLevel, entity.getX(), entity.getY(), entity.getZ(), redPacketStack);
                        itemEntity.setPickUpDelay(10);
                        serverLevel.addFreshEntity(itemEntity);
                    }
                }
            }
        }
    }
}