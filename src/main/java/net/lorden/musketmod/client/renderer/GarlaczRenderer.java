package net.lorden.musketmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.lorden.musketmod.client.model.GarlaczModel;
import net.lorden.musketmod.item.GarlaczItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GarlaczRenderer extends GeoItemRenderer<GarlaczItem> {
    public GarlaczRenderer() {
        super(new GarlaczModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        boolean isAiming = stack.hasTag() && stack.getTag().getBoolean("IsAiming");

        // Pozycja celowania w TPP
        if (transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && isAiming) {
            poseStack.translate(-0.20F, 0.18F, -0.42F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-180.0F));
        }

        // Przyłożenie do oka w FPP
        if (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && isAiming) {
            poseStack.translate(-0.40F, 0.08F, 0.15F);
            poseStack.mulPose(Axis.YP.rotationDegrees(4.0F));
        }

        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
