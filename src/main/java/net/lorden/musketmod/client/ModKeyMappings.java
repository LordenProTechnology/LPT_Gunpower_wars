package net.lorden.musketmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.lorden.musketmod.MusketMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MusketMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyMappings {

    public static final KeyMapping FREELOOK_KEY = new KeyMapping(
            "key.musketmod.freelook",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT, // Lewy Alt
            "key.categories.musketmod"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FREELOOK_KEY);
    }
}