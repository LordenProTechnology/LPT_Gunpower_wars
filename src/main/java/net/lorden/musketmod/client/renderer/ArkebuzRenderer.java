package net.lorden.musketmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.lorden.musketmod.client.model.ArkebuzModel;
import net.lorden.musketmod.item.ArkebuzItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ArkebuzRenderer extends GeoItemRenderer<ArkebuzItem> {
    public ArkebuzRenderer() {
        super(new ArkebuzModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        boolean isAiming = stack.hasTag() && stack.getTag().getBoolean("IsAiming");

        // 1. WIDOK Z TRZECIEJ OSOBY (TPP) W TRAKCIE CELOWANIA
        if (transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && isAiming) {
            // Pozycja na wysokości klatki piersiowej i dłoni kuszy
            poseStack.translate(-0.20F, 0.18F, -0.42F);

            // Neutralizacja kąta 70° z Twojego JSON-a
            poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F));

            // Obrót lufy wprost przed siebie (zamiast w poprzek klatki)
            poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-180.0F));
        }

        // 2. WIDOK Z PIERWSZEJ OSOBY (FPP) W TRAKCIE CELOWANIA
        if (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && isAiming) {
            // Wyśrodkowanie przyrządów celowniczych pod krzyżyk
            poseStack.translate(-0.45F, 0.10F, 0.20F);
            poseStack.mulPose(Axis.YP.rotationDegrees(5.0F));
        }

        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}