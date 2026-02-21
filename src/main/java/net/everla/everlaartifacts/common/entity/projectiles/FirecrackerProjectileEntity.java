package net.everla.everlaartifacts.common.entity.projectiles;

import net.minecraftforge.registries.ForgeRegistries;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.init.EverlaartifactsModEntities;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class FirecrackerProjectileEntity extends AbstractArrow implements ItemSupplier {
	public static final ItemStack PROJECTILE_ITEM = new ItemStack(EverlaartifactsModItems.FIRECRACKER.get());

	public FirecrackerProjectileEntity(PlayMessages.SpawnEntity packet, Level world) {
		super(EverlaartifactsModEntities.FIRECRACKER_PROJECTILE.get(), world);
	}

	public FirecrackerProjectileEntity(EntityType<? extends FirecrackerProjectileEntity> type, Level world) {
		super(type, world);
	}

	public FirecrackerProjectileEntity(EntityType<? extends FirecrackerProjectileEntity> type, double x, double y, double z, Level world) {
		super(type, x, y, z, world);
	}

	public FirecrackerProjectileEntity(EntityType<? extends FirecrackerProjectileEntity> type, LivingEntity entity, Level world) {
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
		entity.setArrowCount(entity.getArrowCount() - 1);
		// 在造成伤害时触发爆炸
		if (!this.level().isClientSide && !this.isRemoved()) {
			explode();
			this.discard();
		}
	}

	@Override
	public void playerTouch(Player entity) {
		super.playerTouch(entity);
		// 避免对自己爆炸，并确保只爆炸一次
		if (this.getOwner() != entity && !this.level().isClientSide && !this.isRemoved()) {
			explode();
			this.discard();
		}
	}

	@Override
	public void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		// 确保只爆炸一次，但允许对大多数实体爆炸
		if (!this.level().isClientSide && !this.isRemoved()) {
			// 检查是否为Owner，避免自爆
			if (this.getOwner() != entityHitResult.getEntity()) {
				explode();
				this.discard();
			}
		}
	}

	@Override
	public void onHitBlock(BlockHitResult blockHitResult) {
		super.onHitBlock(blockHitResult);
		// 确保只爆炸一次
		if (!this.level().isClientSide && !this.isRemoved()) {
			explode();
			this.discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.inGround && !this.level().isClientSide && !this.isRemoved()) {
			// 在地面上停留后爆炸并销毁
			explode();
			this.discard();
		}
	}

	// 爆炸方法
	private void explode() {
		if (!this.level().isClientSide && !this.isRemoved()) {
			// 播放音效
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
				ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:deltarune_explosion")),
				SoundSource.PLAYERS, 1.0F, 1.0F);
			// 小爆炸，不破坏方块
			this.level().explode(null, this.getX(), this.getY(), this.getZ(), 1.0F, false, Level.ExplosionInteraction.NONE);
		}
	}

	public static FirecrackerProjectileEntity shoot(Level world, LivingEntity entity, RandomSource source) {
		return shoot(world, entity, source, 0.3f, 0, 0); // 伤害设为0
	}

	public static FirecrackerProjectileEntity shoot(Level world, LivingEntity entity, RandomSource source, float pullingPower) {
		return shoot(world, entity, source, pullingPower * 0.3f, 0, 0); // 伤害设为0
	}

	public static FirecrackerProjectileEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
		FirecrackerProjectileEntity entityarrow = new FirecrackerProjectileEntity(EverlaartifactsModEntities.FIRECRACKER_PROJECTILE.get(), entity, world);
		entityarrow.shoot(entity.getViewVector(1).x, entity.getViewVector(1).y, entity.getViewVector(1).z, power * 2, 0);
		entityarrow.setSilent(true);
		entityarrow.setCritArrow(false);
		entityarrow.setBaseDamage(damage); // 设置伤害
		entityarrow.setKnockback(knockback);
		entityarrow.setOwner(entity); // 设置Owner避免自爆
		world.addFreshEntity(entityarrow);
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.shoot")), SoundSource.PLAYERS, 1, 1f / (random.nextFloat() * 0.5f + 1) + (power / 2));
		return entityarrow;
	}

	public static FirecrackerProjectileEntity shoot(LivingEntity entity, LivingEntity target) {
		FirecrackerProjectileEntity entityarrow = new FirecrackerProjectileEntity(EverlaartifactsModEntities.FIRECRACKER_PROJECTILE.get(), entity, entity.level());
		double dx = target.getX() - entity.getX();
		double dy = target.getY() + target.getEyeHeight() - 1.1;
		double dz = target.getZ() - entity.getZ();
		entityarrow.shoot(dx, dy - entityarrow.getY() + Math.hypot(dx, dz) * 0.2F, dz, 0.3f * 2, 0); // 伤害设为0
		entityarrow.setSilent(true);
		entityarrow.setBaseDamage(0); // 设置伤害为0
		entityarrow.setKnockback(0);
		entityarrow.setCritArrow(false);
		entityarrow.setOwner(entity); // 设置Owner避免自爆
		entity.level().addFreshEntity(entityarrow);
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.firework_rocket.shoot")), SoundSource.PLAYERS, 1,
				1f / (RandomSource.create().nextFloat() * 0.5f + 1));
		return entityarrow;
	}
}