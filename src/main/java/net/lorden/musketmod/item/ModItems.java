package net.lorden.musketmod.item;

import net.lorden.musketmod.MusketMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // 1. NAJPIERW MUSI BYĆ TWORZONY REJESTR ITEMS:
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MusketMod.MOD_ID);

    // 2. DOPIERO POD SPODEM REJESTRUJESZ PRZEDMIOTY:
    public static final RegistryObject<Item> MUSKET_BALL = ITEMS.register("musket_ball",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MUSKET = ITEMS.register("musket",
            () -> new MusketItem(new Item.Properties().stacksTo(1).durability(250)));

    // Rejestracja Arkebuza:
    public static final RegistryObject<Item> ARKEBUZ = ITEMS.register("arkebuz",
            () -> new ArkebuzItem(new Item.Properties().stacksTo(1).durability(1000)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}