package net.lorden.musketmod.client;

import net.lorden.musketmod.MusketMod;
import net.lorden.musketmod.entity.ModEntities;
import net.lorden.musketmod.item.ArkebuzItem; // <-- TEGO IMPORTU BRAKOWAŁO
import net.lorden.musketmod.item.ModItems;
import net.lorden.musketmod.item.MusketItem;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MusketMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Rejestracja dla Arkebuza (poprawione .get())
            ItemProperties.register(ModItems.ARKEBUZ.get(),
                    new ResourceLocation(MusketMod.MOD_ID, "loaded"),
                    (stack, level, entity, seed) -> ArkebuzItem.isLoaded(stack) ? 1.0F : 0.0F);

            ItemProperties.register(ModItems.GARLACZ.get(),
                    new ResourceLocation(MusketMod.MOD_ID, "loaded"),
                    (stack, level, entity, seed) -> GarlaczItem.isLoaded(stack) ? 1.0F : 0.0F);

            // Rejestracja dla Muszkietu
            ItemProperties.register(ModItems.MUSKET.get(),
                    new ResourceLocation(MusketMod.MOD_ID, "loaded"),
                    (stack, level, entity, seed) -> MusketItem.isLoaded(stack) ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MUSKET_BULLET.get(), ThrownItemRenderer::new);
    }
}