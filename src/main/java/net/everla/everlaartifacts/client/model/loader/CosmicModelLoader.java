package net.everla.everlaartifacts.client.model.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.client.shader.CosmicShaders;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CosmicModelLoader implements IGeometryLoader<CosmicModelLoader.CosmicGeometry> {
    public static final CosmicModelLoader INSTANCE = new CosmicModelLoader();

    @Override
    public CosmicGeometry read(JsonObject modelContents, JsonDeserializationContext deserializationContext) throws JsonParseException {
        EverlaartifactsMod.LOGGER.info("[CosmicModelLoader] Reading model...");
        BlockModel baseModel = deserializationContext.deserialize(clear(modelContents), BlockModel.class);
        EverlaartifactsMod.LOGGER.info("[CosmicModelLoader] Base model loaded, elements count: {}", baseModel.getElements().size());
        List<ResourceLocation> maskTextures = getMasks(modelContents);
        EverlaartifactsMod.LOGGER.info("[CosmicModelLoader] Mask textures: {}", maskTextures);
        return new CosmicGeometry(baseModel, maskTextures);
    }

    private static JsonObject clear(JsonObject modelContents) {
        JsonObject clean = modelContents.deepCopy();
        clean.remove("loader");
        clean.remove("cosmic");
        return clean;
    }

    private static List<ResourceLocation> getMasks(JsonObject modelContents) {
        JsonObject cosmic = modelContents.getAsJsonObject("cosmic");
        if (cosmic == null) {
            throw new IllegalStateException("Missing cosmic object.");
        }

        List<ResourceLocation> masks = new ArrayList<>();
        if (cosmic.has("mask") && cosmic.get("mask").isJsonArray()) {
            JsonArray array = cosmic.getAsJsonArray("mask");
            for (JsonElement element : array) {
                masks.add(new ResourceLocation(element.getAsString()));
            }
        } else {
            masks.add(new ResourceLocation(GsonHelper.getAsString(cosmic, "mask")));
        }
        return masks;
    }

    public static class CosmicGeometry implements IUnbakedGeometry<CosmicGeometry> {
        private final BlockModel baseModel;
        private final List<ResourceLocation> maskTextures;

        public CosmicGeometry(BlockModel baseModel, List<ResourceLocation> maskTextures) {
            this.baseModel = baseModel;
            this.maskTextures = maskTextures;
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            EverlaartifactsMod.LOGGER.info("[CosmicGeometry] Baking model at: {}", modelLocation);
            // 直接使用 baseModel.bake()，因为模型已经没有 parent 了
            BakedModel bakedBase = baseModel.bake(baker, baseModel, spriteGetter, modelState, modelLocation, true);
            EverlaartifactsMod.LOGGER.info("[CosmicGeometry] Base model baked: {}", bakedBase != null ? "OK" : "NULL");
            if (bakedBase != null) {
                EverlaartifactsMod.LOGGER.info("[CosmicGeometry] Base model has quads: {}", !bakedBase.getQuads(null, null, null).isEmpty());
            }
            // 注意：不在此处加载 mask sprites，因为它们需要从 cosmic atlas 获取
            // 而 atlas 可能还没有准备好，所以延迟到渲染时再加载
            return new CosmicBakedModel(bakedBase, maskTextures);
        }
    }
}
