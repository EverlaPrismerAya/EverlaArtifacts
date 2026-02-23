package net.everla.everlaartifacts.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.everla.everlaartifacts.common.entity.bosses.watari_nina.WatariNinaEntity;

@OnlyIn(Dist.CLIENT)
public class WatariNinaRenderer extends MobRenderer<WatariNinaEntity, PlayerModel<WatariNinaEntity>> {
    public WatariNinaRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WatariNinaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(EverlaartifactsMod.MODID, "textures/entity/watari_nina.png");
    }
}