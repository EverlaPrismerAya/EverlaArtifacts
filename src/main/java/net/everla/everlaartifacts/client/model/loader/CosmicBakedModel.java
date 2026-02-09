package net.everla.everlaartifacts.client.model.loader;

import com.mojang.math.Transformation;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.client.shader.CosmicRenderTypes;
import net.everla.everlaartifacts.client.shader.CosmicShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.SimpleModelState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CosmicBakedModel implements BakedModel {
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ModelState IDENTITY_STATE = new SimpleModelState(Transformation.identity());

    private final BakedModel wrapped;
    private final List<ResourceLocation> maskTextureLocs;
    private List<TextureAtlasSprite> maskSprites;
    private List<BakedQuad> maskQuads;
    private final BakedModel maskModel;
    private final ItemOverrides overrides;

    public CosmicBakedModel(BakedModel wrapped, List<ResourceLocation> maskTextureLocs) {
        EverlaartifactsMod.LOGGER.info("[CosmicBakedModel] Constructor called, wrapped has quads: {}", 
            !wrapped.getQuads(null, null, null).isEmpty());
        this.wrapped = wrapped;
        this.maskTextureLocs = maskTextureLocs;
        this.maskSprites = null;
        this.maskQuads = null;
        this.maskModel = new MaskModel();
        this.overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
                BakedModel resolved = CosmicBakedModel.this.wrapped.getOverrides().resolve(CosmicBakedModel.this.wrapped, stack, world, entity, seed);
                if (resolved == CosmicBakedModel.this.wrapped) {
                    return CosmicBakedModel.this;
                }
                return new CosmicBakedModel(resolved, CosmicBakedModel.this.maskTextureLocs);
            }
        };
    }

    private void ensureMaskQuads() {
        if (maskQuads == null) {
            Minecraft mc = Minecraft.getInstance();
            try {
                var atlas = mc.getTextureAtlas(CosmicShaders.COSMIC_ATLAS);
                if (atlas != null) {
                    maskSprites = new ArrayList<>();
                    for (ResourceLocation maskLoc : maskTextureLocs) {
                        TextureAtlasSprite sprite = atlas.apply(maskLoc);
                        if (sprite != null) {
                            maskSprites.add(sprite);
                        }
                    }
                    maskQuads = bakeItem(maskSprites);
                    EverlaartifactsMod.LOGGER.info("[CosmicBakedModel] Lazy loaded {} mask sprites and {} quads", maskSprites.size(), maskQuads.size());
                } else {
                    EverlaartifactsMod.LOGGER.warn("[CosmicBakedModel] Cosmic atlas is null, using empty quads");
                    maskQuads = Collections.emptyList();
                }
            } catch (Exception e) {
                EverlaartifactsMod.LOGGER.error("[CosmicBakedModel] Failed to load mask quads: {}", e.getMessage());
                maskQuads = Collections.emptyList();
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        List<BakedQuad> quads = wrapped.getQuads(state, side, rand);
        if (side == null && rand == null) {
            EverlaartifactsMod.LOGGER.info("[CosmicBakedModel.getQuads] Returning {} quads", quads.size());
        }
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return wrapped.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return wrapped.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return wrapped.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return wrapped.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return wrapped.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public ItemTransforms getTransforms() {
        return wrapped.getTransforms();
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        List<BakedModel> passes = new ArrayList<>(wrapped.getRenderPasses(stack, fabulous));
        passes.add(maskModel);
        return passes;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return wrapped.getRenderTypes(stack, fabulous);
    }

    private class MaskModel implements BakedModel {
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side != null) {
                return Collections.emptyList();
            }
            ensureMaskQuads();
            return maskQuads != null ? maskQuads : Collections.emptyList();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return wrapped.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return wrapped.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return wrapped.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return wrapped.getParticleIcon();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public ItemTransforms getTransforms() {
            return wrapped.getTransforms();
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return List.of(CosmicRenderTypes.COSMIC);
        }
    }

    public static List<BakedQuad> bakeItem(List<TextureAtlasSprite> sprites) {
        List<BakedQuad> quads = new ArrayList<>();
        for (int layer = 0; layer < sprites.size(); layer++) {
            TextureAtlasSprite sprite = sprites.get(layer);
            List<BlockElement> unbaked = ITEM_MODEL_GENERATOR.processFrames(layer, "layer" + layer, sprite.contents());
            for (BlockElement element : unbaked) {
                for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                    quads.add(FACE_BAKERY.bakeQuad(
                        element.from,
                        element.to,
                        entry.getValue(),
                        sprite,
                        entry.getKey(),
                        IDENTITY_STATE,
                        element.rotation,
                        element.shade,
                        new ResourceLocation(EverlaartifactsMod.MODID, "cosmic_mask")
                    ));
                }
            }
        }
        return quads;
    }
}
