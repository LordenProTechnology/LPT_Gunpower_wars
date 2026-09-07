package net.lorden.musketmod.client;

import net.lorden.musketmod.MusketMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MusketMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    private static ItemStack cachedOffhand = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.OFF_HAND) {
            Player player = Minecraft.getInstance().player;
            if (player != null && isGunLoading(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();

            // 1. Obsługa znikania pobojczyka podczas ładowania
            if (isGunLoading(player)) {
                cachedOffhand = player.getOffhandItem();
                if (!cachedOffhand.isEmpty()) {
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                }
                if (event.getRenderer().getModel() instanceof PlayerModel<?> playerModel) {
                    playerModel.leftArmPose = HumanoidModel.ArmPose.EMPTY;
                    playerModel.leftArm.visible = true;
                    playerModel.leftSleeve.visible = true;
                }
            }

            // 2. Pozycja trzymania kuszy w trakcie celowania (aim)
            if (mainHand.hasTag() && mainHand.getTag().getBoolean("IsAiming")) {
                if (event.getRenderer().getModel() instanceof PlayerModel<?> playerModel) {
                    boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;
                    if (isRightHanded) {
                        playerModel.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
                    } else {
                        playerModel.leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            if (!cachedOffhand.isEmpty()) {
                player.setItemInHand(InteractionHand.OFF_HAND, cachedOffhand);
                cachedOffhand = ItemStack.EMPTY;
            }
        }
    }

    private static boolean isGunLoading(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.hasTag() && main.getTag().getBoolean("IsLoading");
    }
}