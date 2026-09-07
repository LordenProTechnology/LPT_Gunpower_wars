package net.lorden.musketmod.client;

import net.lorden.musketmod.MusketMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.InteractionHand;
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

    // 1. Pierwsza osoba (FPP): ukrywa wyłącznie rendering lewej ręki/przedmiotu offhand
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.OFF_HAND) {
            Player player = Minecraft.getInstance().player;
            if (player != null && isGunLoading(player)) {
                // Jeśli lewa ręka trzyma pobojczyk, anulujemy wyłącznie jej wyrenderowanie w FPP
                event.setCanceled(true);
            }
        }
    }

    // 2. Trzecia osoba (TPP): tymczasowo opróżnia lewą rękę tuż przed renderowaniem modelu gracza
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            if (isGunLoading(player)) {
                // Zapamiętujemy przedmiot z lewej ręki i wstawiamy pusty ItemStack
                cachedOffhand = player.getOffhandItem();
                if (!cachedOffhand.isEmpty()) {
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                }

                // Wymuszamy, by lewa ręka zwisała/ruszała się normalnie, a nie w geście trzymania przedmiotu
                if (event.getRenderer().getModel() instanceof PlayerModel<?> playerModel) {
                    playerModel.leftArmPose = HumanoidModel.ArmPose.EMPTY;
                    playerModel.leftArm.visible = true;
                    playerModel.leftSleeve.visible = true;
                }
            }
        }
    }

    // Przywracamy pobojczyk do lewej ręki zaraz po narysowaniu klatki modelu
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