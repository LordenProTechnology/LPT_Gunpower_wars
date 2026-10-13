package net.lorden.musketmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.lorden.musketmod.item.ArkebuzItem;
import net.lorden.musketmod.item.ModItems;
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
    // PANEL POZYCJI PRZY PASIE
    // ==========================================

    // Wysunięcie lekko na zewnątrz prawego biodra (żeby nie tkwiła w środku ciała)
    private static final float OFFSET_X = 0.25F;  // Prawo (+) / Lewo (-)
    private static final float OFFSET_Y = 0.70F;  // W dół (+) w stronę pasa
    private static final float OFFSET_Z = 0.00F;  // Przód / tył

    private static final float SCALE = 1.0F;

    // Obrócenie do góry nogami (oś Z lub X)
    private static final float ROTATION_X = 0.0F;
    private static final float ROTATION_Y = 0.0F;
    private static final float ROTATION_Z = 180.0F;

    public PowderFlaskLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 1. Sprawdzamy ręce - ukrywamy TYLKO wtedy, gdy przedmiot w ręku faktycznie istnieje i jest prochownicą
        if (!mainHand.isEmpty() && mainHand.is(ModItems.POWDER_FLASK.get())) {
            return;
        }
        if (!offHand.isEmpty() && offHand.is(ModItems.POWDER_FLASK.get())) {
            return;
        }

        // 2. Pobieramy prochownicę z ekwipunku/hotbara
        ItemStack flask = ArkebuzItem.getPowderFlaskFromHotbar(player);
        if (flask == null || flask.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // 3. Podpięcie pod kość korpusu gracza
        this.getParentModel().body.translateAndRotate(poseStack);

        // 4. Pozycja
        poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z);

        // 5. Skala
        poseStack.scale(SCALE, SCALE, SCALE);

        // 6. Obrót
        if (ROTATION_X != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(ROTATION_X));
        if (ROTATION_Y != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(ROTATION_Y));
        if (ROTATION_Z != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(ROTATION_Z));

        // 7. Renderowanie
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