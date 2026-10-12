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

    // ==========================================
    // PANEL EDYCJI POZYCJI PROCHOWNICY PRZY PASIE
    // ==========================================

    // Przesunięcie (pozycja)
    private static final float OFFSET_X = 0.28F;  // Prawo (+) / Lewo (-) względem gracza
    private static final float OFFSET_Y = 0.75F;  // W dół (+) / W górę (-) (wysokość pasa)
    private static final float OFFSET_Z = 0.00F;  // Do tyłu (+) / Do przodu (-)

    // Skala (1.0F = 100% rozmiaru z Blockbencha)
    private static final float SCALE = 1.0F;

    // Obrót (kąty w stopniach)
    private static final float ROTATION_X = 0.0F;   // Pochylenie przód/tył
    private static final float ROTATION_Y = 0.0F;   // Obrót lewo/prawo (ustawia cienką ściankę w przód)
    private static final float ROTATION_Z = 0.0F;   // Przechył na bok

    public PowderFlaskLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack flask = ArkebuzItem.getPowderFlaskFromHotbar(player);
        if (flask == null || flask.isEmpty()) return;

        poseStack.pushPose();

        // 1. Przypięcie do korpusu (obraca się i pochyla razem z tułowiem)
        this.getParentModel().body.translateAndRotate(poseStack);

        // 2. Pozycja przy pasie
        poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z);

        // 3. Rozmiar (100% bazowego modelu)
        poseStack.scale(SCALE, SCALE, SCALE);

        // 4. Rotacja (ustawianie cienką ścianką do przodu)
        if (ROTATION_X != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(ROTATION_X));
        if (ROTATION_Y != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(ROTATION_Y));
        if (ROTATION_Z != 90.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(ROTATION_Z));

        // 5. Renderowanie modelu GeckoLib
        Minecraft.getInstance().getItemRenderer().renderStatic(
                flask,
                ItemDisplayContext.NONE,
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