package net.everla.everlaartifacts.client.model.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.everla.everlaartifacts.client.model.loader.base.BaseGeometry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Forge geometry model loader for "everlaartifacts:cosmic".
 * <p>
 * Reads a model JSON like:
 * <pre>{@code
 * {
 *   "parent": "item/handheld",
 *   "textures": { "layer0": "..." },
 *   "loader": "everlaartifacts:cosmic",
 *   "cosmic": {
 *     "mask": "everlaartifacts:mask/item/procedure_sword"
 *   }
 * }
 * }</pre>
 * <p>
 * The {@code "cosmic"} key is stripped before the base model is baked.
 * The mask texture(s) are passed to {@link CosmicBakeModel} for use
 * with the cosmic shader.
 * <p>
 * Based on Avaritia's CosmicModelLoader implementation.
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2026 Nova-Committee
 */
public class CosmicModelLoader implements IGeometryLoader<CosmicModelLoader.CosmicGeometry> {

    public static final CosmicModelLoader INSTANCE = new CosmicModelLoader();

    @Override
    public CosmicGeometry read(JsonObject modelContents, JsonDeserializationContext deserializationContext)
            throws JsonParseException {

        // Strip the "cosmic" key and deserialize the rest as a vanilla BlockModel
        BlockModel baseModel = deserializationContext.deserialize(
                clear(modelContents, "cosmic"), BlockModel.class);

        // Extract mask texture(s) from the "cosmic" section
        List<ResourceLocation> cosmicMaskTextures = getMasks(modelContents, "cosmic");

        return new CosmicGeometry(baseModel, cosmicMaskTextures);
    }

    /**
     * Strips the "loader" key and any additional type keys from the model JSON,
     * leaving a clean vanilla BlockModel JSON.
     */
    private static JsonObject clear(JsonObject modelContents, String... types) {
        final JsonObject clean = modelContents.deepCopy();
        clean.remove("loader");
        for (String type : types) {
            clean.remove(type);
        }
        return clean;
    }

    /**
     * Extracts mask texture ResourceLocations from a section of the model JSON.
     * Supports both a single mask string and an array of mask strings.
     *
     * @param modelContents the full model JSON
     * @param type          the section key (e.g. "cosmic")
     * @return list of ResourceLocations for mask textures
     */
    private static List<ResourceLocation> getMasks(JsonObject modelContents, String type) {
        final JsonObject section = modelContents.getAsJsonObject(type);
        if (section == null) {
            throw new IllegalStateException("Missing '" + type + "' object in model JSON.");
        }
        List<ResourceLocation> maskTextures = new ArrayList<>();
        if (section.has("mask") && section.get("mask").isJsonArray()) {
            JsonArray masks = section.getAsJsonArray("mask");
            for (int i = 0; i < masks.size(); i++) {
                maskTextures.add(new ResourceLocation(masks.get(i).getAsString()));
            }
        } else {
            maskTextures.add(new ResourceLocation(GsonHelper.getAsString(section, "mask")));
        }
        return maskTextures;
    }

    // ---- Geometry inner class ----

    public static class CosmicGeometry extends BaseGeometry<CosmicGeometry> {

        private final List<ResourceLocation> maskTextures;

        public CosmicGeometry(final BlockModel baseModel, final List<ResourceLocation> maskTextures) {
            super(baseModel);
            this.maskTextures = maskTextures;
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides,
                               ResourceLocation modelLocation) {
            // Bake the base model (the sword with its layer0 texture)
            BakedModel baseBakedModel = this.baseModel.bake(
                    baker, this.baseModel, spriteGetter, modelState, modelLocation, true);

            // Wrap in CosmicBakeModel for the starry sky overlay
            return new CosmicBakeModel(baseBakedModel, maskTextures);
        }
    }
}
