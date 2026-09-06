package net.lorden.musketmod.client.model;

import net.lorden.musketmod.MusketMod;
import net.lorden.musketmod.item.ArkebuzItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ArkebuzModel extends GeoModel<ArkebuzItem> {
    @Override
    public ResourceLocation getModelResource(ArkebuzItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "geo/arkebuz.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ArkebuzItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "textures/item/arkebuz.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ArkebuzItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "animations/arkebuz.animation.json");
    }
}