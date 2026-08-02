package net.everla.everlaartifacts.client.model.loader.base;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

/**
 * Base geometry class for custom model loaders.
 * Holds the vanilla BlockModel parsed from the model JSON
 * and delegates parent resolution to it.
 * <p>
 * Based on Avaritia's BaseGeometry implementation.
 */
public abstract class BaseGeometry<U extends IUnbakedGeometry<U>> implements IUnbakedGeometry<U> {

    public final BlockModel baseModel;

    public BaseGeometry(BlockModel baseModel) {
        this.baseModel = baseModel;
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter,
                                IGeometryBakingContext context) {
        baseModel.resolveParents(modelGetter);
    }
}
