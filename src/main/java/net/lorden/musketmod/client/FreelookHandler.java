package net.lorden.musketmod.client;

import net.lorden.musketmod.MusketMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MusketMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FreelookHandler {

    private static boolean wasAltDown = false;
    private static float lockedVehicleYRot = 0.0F;

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Entity vehicle = player.getVehicle();

        // Sprawdzamy, czy gracz jedzie na wierzchowcu (LivingEntity)
        if (vehicle instanceof LivingEntity mount) {
            boolean isAltDown = ModKeyMappings.FREELOOK_KEY.isDown();

            if (isAltDown) {
                // W pierwszym ticku wciśnięcia Alta zapamiętujemy kierunek wierzchowca
                if (!wasAltDown) {
                    lockedVehicleYRot = mount.getYRot();
                    wasAltDown = true;
                }

                // Wymuszamy na wierzchowcu zachowanie stałego kąta
                mount.setYRot(lockedVehicleYRot);
                mount.yRotO = lockedVehicleYRot;
                mount.setYHeadRot(lockedVehicleYRot);
                mount.yHeadRotO = lockedVehicleYRot;
                mount.yBodyRot = lockedVehicleYRot;
                mount.yBodyRotO = lockedVehicleYRot;

                // Ograniczenie obrotu głowy gracza do max 85 stopni w lewo/prawo od osi konia
                float angleDiff = player.getYRot() - lockedVehicleYRot;
                while (angleDiff > 180.0F) angleDiff -= 360.0F;
                while (angleDiff < -180.0F) angleDiff += 360.0F;

                if (angleDiff > 85.0F) {
                    player.setYRot(lockedVehicleYRot + 85.0F);
                } else if (angleDiff < -85.0F) {
                    player.setYRot(lockedVehicleYRot - 85.0F);
                }
            } else {
                wasAltDown = false;
            }
        } else {
            wasAltDown = false;
        }
    }
}