package net.everla.everlaartifacts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.everla.everlaartifacts.client.model.ModelDanmaku;
import net.everla.everlaartifacts.common.entity.projectiles.DanmakuEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class DanmakuRenderer extends EntityRenderer<DanmakuEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("everlaartifacts", "textures/entity/danmaku.png");
    private final ModelDanmaku<DanmakuEntity> model;

    public DanmakuRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ModelDanmaku<>(context.bakeLayer(ModelDanmaku.LAYER_LOCATION));
    }

    @Override
    public void render(DanmakuEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // 设置位置
        poseStack.translate(0.0D, 0.15D, 0.0D);
        
        // 计算旋转
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F;
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        
        // 应用旋转
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        
        // 缩放
        poseStack.scale(1.0F, 1.0F, 1.0F);
        
        // 渲染模型
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected int getBlockLightLevel(DanmakuEntity entity, BlockPos pos) {
        return 15; // 最大亮度
    }

    @Override
    public ResourceLocation getTextureLocation(DanmakuEntity entity) {
        return TEXTURE;
    }
}