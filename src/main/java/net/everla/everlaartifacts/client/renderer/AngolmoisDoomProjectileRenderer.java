package net.everla.everlaartifacts.client.renderer;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import net.everla.everlaartifacts.entity.projectiles.AngolmoisDoomProjectileEntity;
import net.everla.everlaartifacts.client.model.Modelangolmois_doom;

import com.mojang.math.Axis;

public class AngolmoisDoomProjectileRenderer extends EntityRenderer<AngolmoisDoomProjectileEntity> {
	private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("everlaartifacts", "textures/entity/angolmois_doom.png");
	private final Modelangolmois_doom<AngolmoisDoomProjectileEntity> model;

	public AngolmoisDoomProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new Modelangolmois_doom<>(context.bakeLayer(Modelangolmois_doom.LAYER_LOCATION));
	}

	@Override
	public void render(AngolmoisDoomProjectileEntity entityIn, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
		poseStack.pushPose();
		
		// 设置旋转和位置
		poseStack.translate(0.0D, 0.1D, 0.0D);
		
		// 根据实体运动方向设置朝向
		poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
		
		// 缩放模型
		float scale = 2.0F;
		poseStack.scale(scale, scale, scale);
		
		// 渲染发光模型
		com.mojang.blaze3d.vertex.VertexConsumer vertexconsumer = bufferIn.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entityIn)));
		this.model.renderToBuffer(poseStack, vertexconsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		
		poseStack.popPose();
		super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
	}

	@Override
	public ResourceLocation getTextureLocation(AngolmoisDoomProjectileEntity entity) {
		return texture;
	}
}