package net.lorden.musketmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.lorden.musketmod.item.ArkebuzItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PowderFlaskLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PowderFlaskLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack flask = ArkebuzItem.getPowderFlaskFromHotbar(player);
        if (flask == null || flask.isEmpty()) return;

        poseStack.pushPose();
        // Przypięcie do korpusu (body) modelu gracza
        this.getParentModel().body.translateAndRotate(poseStack);

        // Ustawienie na lewym biodrze przy pasie
        poseStack.translate(0.25F, 0.70F, 0.0F);
        poseStack.scale(0.40F, 0.40F, 0.40F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                flask,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                player.level(),
                player.getId()
        );

        poseStack.popPose();
    }
}