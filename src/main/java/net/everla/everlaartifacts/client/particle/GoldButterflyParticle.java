
package net.everla.everlaartifacts.client.particle;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.multiplayer.ClientLevel;

@OnlyIn(Dist.CLIENT)
public class GoldButterflyParticle extends TextureSheetParticle {
	public static GoldButterflyParticleProvider provider(SpriteSet spriteSet) {
		return new GoldButterflyParticleProvider(spriteSet);
	}

	public static class GoldButterflyParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public GoldButterflyParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new GoldButterflyParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;

	protected GoldButterflyParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.setSize(0.05f, 0.05f);
		this.lifetime = (int) Math.max(1, 20 + (this.random.nextInt(10) - 5));
		this.gravity = -0.05f;
		this.hasPhysics = false;
		this.xd = vx * 0.1;
		this.yd = vy * 0.1;
		this.zd = vz * 0.1;
		this.setSpriteFromAge(spriteSet);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 15728880;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.removed) {
			// 添加随机动量以实现蝴蝶飞舞效果
			// 随机改变粒子的运动方向
			if (this.random.nextFloat() < 0.9) { // 90%的概率改变方向，降低频率
				this.xd += (this.random.nextDouble() - 0.5) * 0.02;
				this.yd += (this.random.nextDouble() - 0.5) * 0.02;
				this.zd += (this.random.nextDouble() - 0.5) * 0.02;
				
				// 限制速度，避免过快
				double speedLimit = 0.05;
				if (Math.abs(this.xd) > speedLimit) this.xd = Math.copySign(speedLimit, this.xd);
				if (Math.abs(this.yd) > speedLimit) this.yd = Math.copySign(speedLimit, this.yd);
				if (Math.abs(this.zd) > speedLimit) this.zd = Math.copySign(speedLimit, this.zd);
			}
			
			// 增加一些随机的漂浮效果
			if (this.random.nextFloat() < 0.05) {
				this.yd += 0.005; // 较小的随机向上推力
			}
			
			this.setSprite(this.spriteSet.get((this.age / 1) % 2 + 1, 2));
		}
	}
}