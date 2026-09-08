package net.lorden.musketmod.client;

import net.lorden.musketmod.MusketMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
            // Ukrywa dłoń i pobojczyk w FPP podczas ładowania ORAZ celowania
            if (player != null && shouldHideOffhand(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();

            // 1. Schowanie pobojczyka w TPP w trakcie ładowania lub celowania
            if (shouldHideOffhand(player)) {
                cachedOffhand = player.getOffhandItem();
                if (!cachedOffhand.isEmpty()) {
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                }
            }

            // 2. Pozycja rąk jak przy kuszy w trakcie celowania
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

    private static boolean shouldHideOffhand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.hasTag()) {
            return main.getTag().getBoolean("IsLoading") || main.getTag().getBoolean("IsAiming");
        }
        return false;
    }

    // Rejestracja warstwy renderowania prochownicy na szynie MOD
    @Mod.EventBusSubscriber(modid = MusketMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.AddLayers event) {
            for (String skinType : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skinType);
                if (renderer != null) {
                    renderer.addLayer(new PowderFlaskLayer(renderer));
                }
            }
        }
    }
}