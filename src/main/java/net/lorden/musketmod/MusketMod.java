package net.lorden.musketmod;

import net.lorden.musketmod.entity.ModEntities;
import net.lorden.musketmod.item.ModCreativeModeTabs;
import net.lorden.musketmod.item.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MusketMod.MOD_ID)
public class MusketMod {
    public static final String MOD_ID = "musketmod";

    public MusketMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(bus);
        ModItems.register(bus);
        ModCreativeModeTabs.register(bus);

        MinecraftForge.EVENT_BUS.register(this);
    }
}