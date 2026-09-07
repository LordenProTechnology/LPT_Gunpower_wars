package net.lorden.musketmod.client;

import net.lorden.musketmod.MusketMod;
import net.minecraft.client.Minecraft;
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

    // 1. Ukrywanie dłoni w pierwszej osobie (FPP)
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.OFF_HAND) {
            Player player = Minecraft.getInstance().player;
            if (player != null && isGunLoading(player)) {
                event.setCanceled(true);
            }
        }
    }

    // 2. Ukrywanie lewej dłoni / przedmiotu w trzeciej osobie (TPP)
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getRenderer().getModel() instanceof PlayerModel<?> playerModel) {
                if (isGunLoading(player)) {
                    // Ukrywa całą lewą rękę i jej rękaw na modelu gracza na czas ładowania
                    playerModel.leftArm.visible = false;
                    playerModel.leftSleeve.visible = false;
                } else {
                    playerModel.leftArm.visible = true;
                    playerModel.leftSleeve.visible = true;
                }
            }
        }
    }

    private static boolean isGunLoading(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.hasTag() && main.getTag().getBoolean("IsLoading");
    }
}