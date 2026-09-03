package net.lorden.musketmod.entity;

import net.lorden.musketmod.MusketMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MusketMod.MOD_ID);

    public static final RegistryObject<EntityType<MusketBulletEntity>> MUSKET_BULLET = ENTITY_TYPES.register("musket_bullet",
            () -> EntityType.Builder.<MusketBulletEntity>of(MusketBulletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("musket_bullet"));

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}