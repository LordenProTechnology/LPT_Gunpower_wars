package net.lorden.musketmod.item;

import net.lorden.musketmod.MusketMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MusketMod.MOD_ID);
    // Amunicja i materiały eksploatacyjne
    public static final RegistryObject<Item> MUSKET_BALL = ITEMS.register("musket_ball",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PAPER_CARTRIDGE = ITEMS.register("paper_cartridge",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LEAD_SHOT = ITEMS.register("lead_shot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAMROD = ITEMS.register("ramrod",
            () -> new Item(new Item.Properties().durability(128)));
    // Broń Dystansowa
    public static final RegistryObject<Item> MUSKET = ITEMS.register("musket",
            () -> new MusketItem(new Item.Properties().stacksTo(1).durability(250)));

    // Rejestracja Arkebuza:
    public static final RegistryObject<Item> ARKEBUZ = ITEMS.register("arkebuz",
            () -> new ArkebuzItem(new Item.Properties().stacksTo(1).durability(1000)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}