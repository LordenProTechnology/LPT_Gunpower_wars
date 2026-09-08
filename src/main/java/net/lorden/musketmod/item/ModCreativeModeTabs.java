package net.lorden.musketmod.item;

import net.lorden.musketmod.MusketMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MusketMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> LORDEN_TAB = CREATIVE_MODE_TABS.register("lorden_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MUSKET.get()))
                    .title(Component.translatable("creativetab.lorden_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MUSKET.get());
                        output.accept(ModItems.MUSKET_BALL.get());
                        output.accept(ModItems.PAPER_CARTRIDGE.get());
                        output.accept(ModItems.POWDER_FLASK.get());
                        output.accept(ModItems.LEAD_SHOT.get());
                        output.accept(ModItems.RAMROD.get());
                        output.accept(ModItems.ARKEBUZ.get());
                        output.accept(ModItems.GARLACZ.get());
                        output.accept(ModItems.HEAVY_MUSKET.get());
                        // TU DODAJESZ SZAFER
                    })
                    .build());

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}