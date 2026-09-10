package net.lorden.musketmod.client.model;

import net.lorden.musketmod.MusketMod;
import net.lorden.musketmod.item.GarlaczItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GarlaczModel extends GeoModel<GarlaczItem> {
    @Override
    public ResourceLocation getModelResource(GarlaczItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "geo/garlacz.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GarlaczItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "textures/item/garlacz.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GarlaczItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "animations/garlacz.animation.json");
    }
}