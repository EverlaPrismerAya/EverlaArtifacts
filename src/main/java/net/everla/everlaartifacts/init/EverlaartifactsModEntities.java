
/*
*    MCreator note: This file will be REGENERATED on each build.
*/
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;

import net.everla.everlaartifacts.entity.projectiles.FirecrackerProjectileEntity;
import net.everla.everlaartifacts.entity.projectiles.AngolmoisDoomProjectileEntity;
import net.everla.everlaartifacts.EverlaartifactsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EverlaartifactsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EverlaartifactsMod.MODID);
	public static final RegistryObject<EntityType<FirecrackerProjectileEntity>> FIRECRACKER_PROJECTILE = register("firecracker_projectile", EntityType.Builder.<FirecrackerProjectileEntity>of(FirecrackerProjectileEntity::new, MobCategory.MISC)
			.setCustomClientFactory(FirecrackerProjectileEntity::new).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	// Start of user code block custom entities
	public static final RegistryObject<EntityType<AngolmoisDoomProjectileEntity>> ANGOLMOIS_DOOM_PROJECTILE = register("angolmois_doom_projectile",
			EntityType.Builder.<AngolmoisDoomProjectileEntity>of(AngolmoisDoomProjectileEntity::new, MobCategory.MISC).setCustomClientFactory(AngolmoisDoomProjectileEntity::new).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
					.setUpdateInterval(1).sized(1.0f, 1.0f));

	// End of user code block custom entities
	private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}
}
