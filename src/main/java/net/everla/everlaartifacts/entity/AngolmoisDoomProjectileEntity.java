package net.everla.everlaartifacts.entity;

import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.RandomSource;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;

import net.everla.everlaartifacts.server.handlers.items.venus_shell.VenusShellAngolmoisHandler;
import net.everla.everlaartifacts.init.EverlaartifactsModEntities;
import net.minecraft.server.level.ServerLevel;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class AngolmoisDoomProjectileEntity extends AbstractArrow implements ItemSupplier {
	public static final ItemStack PROJECTILE_ITEM = ItemStack.EMPTY;

	public AngolmoisDoomProjectileEntity(PlayMessages.SpawnEntity packet, Level world) {
		super(EverlaartifactsModEntities.ANGOLMOIS_DOOM_PROJECTILE.get(), world);
	}

	public AngolmoisDoomProjectileEntity(EntityType<? extends AngolmoisDoomProjectileEntity> type, Level world) {
		super(type, world);
	}

	public AngolmoisDoomProjectileEntity(EntityType<? extends AngolmoisDoomProjectileEntity> type, double x, double y, double z, Level world) {
		super(type, x, y, z, world);
	}

	public AngolmoisDoomProjectileEntity(EntityType<? extends AngolmoisDoomProjectileEntity> type, LivingEntity entity, Level world) {
		super(type, entity, world);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public ItemStack getItem() {
		return PROJECTILE_ITEM;
	}

	@Override
	protected ItemStack getPickupItem() {
		return PROJECTILE_ITEM;
	}

	@Override
	protected void doPostHurtEffects(LivingEntity entity) {
		super.doPostHurtEffects(entity);
		// 移除箭矢计数减少
	}

	@Override
	public void playerTouch(Player entity) {
		super.playerTouch(entity);
		explode();
	}

	@Override
	public void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		explode();
	}

	@Override
	public void onHitBlock(BlockHitResult blockHitResult) {
		super.onHitBlock(blockHitResult);
		explode();
	}

	private void explode() {
		if (!this.level().isClientSide) {
			// 检查是否为Venus Shell的特殊弹射物
			if (this.getPersistentData().contains("IsExplosiveAngolmois")) {
				// 调用自定义爆炸处理
				VenusShellAngolmoisHandler.handleAngolmoisExplosion(
					(ServerLevel) this.level(), 
					this, 
					this.position()
				);
				this.discard();
			} else {
				// 默认爆炸行为
				this.level().explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.MOB);
				this.discard();
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.inGround)
			this.discard();
	}

	public static AngolmoisDoomProjectileEntity shoot(Level world, LivingEntity entity, RandomSource source) {
		return shoot(world, entity, source, 1.0f, 5, 1);
	}

	public static AngolmoisDoomProjectileEntity shoot(Level world, LivingEntity entity, RandomSource source, float pullingPower) {
		return shoot(world, entity, source, pullingPower * 1.0f, 5, 1);
	}

	public static AngolmoisDoomProjectileEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
		AngolmoisDoomProjectileEntity entityarrow = new AngolmoisDoomProjectileEntity(EverlaartifactsModEntities.ANGOLMOIS_DOOM_PROJECTILE.get(), entity, world);
		entityarrow.shoot(entity.getViewVector(1).x, entity.getViewVector(1).y, entity.getViewVector(1).z, power * 2, 0);
		entityarrow.setSilent(true);
		entityarrow.setCritArrow(false);
		entityarrow.setBaseDamage(damage);
		entityarrow.setKnockback(knockback);
		world.addFreshEntity(entityarrow);
		// 移除箭矢发射音效
		return entityarrow;
	}

	public static AngolmoisDoomProjectileEntity shoot(LivingEntity entity, LivingEntity target) {
		AngolmoisDoomProjectileEntity entityarrow = new AngolmoisDoomProjectileEntity(EverlaartifactsModEntities.ANGOLMOIS_DOOM_PROJECTILE.get(), entity, entity.level());
		double dx = target.getX() - entity.getX();
		double dy = target.getY() + target.getEyeHeight() - 1.1;
		double dz = target.getZ() - entity.getZ();
		entityarrow.shoot(dx, dy - entityarrow.getY() + Math.hypot(dx, dz) * 0.2F, dz, 1.0f * 2, 12.0F);
		entityarrow.setSilent(true);
		entityarrow.setBaseDamage(5);
		entityarrow.setKnockback(1);
		entityarrow.setCritArrow(false);
		entity.level().addFreshEntity(entityarrow);
		// 移除箭矢发射音效
		return entityarrow;
	}
}