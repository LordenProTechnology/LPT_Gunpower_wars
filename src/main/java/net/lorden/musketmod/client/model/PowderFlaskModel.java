package net.lorden.musketmod.client.model;

import net.lorden.musketmod.MusketMod;
import net.lorden.musketmod.item.PowderFlaskItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PowderFlaskModel extends GeoModel<PowderFlaskItem> {
    @Override
    public ResourceLocation getModelResource(PowderFlaskItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "geo/powder_flask.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PowderFlaskItem animatable) {
        return new ResourceLocation(MusketMod.MOD_ID, "textures/item/prochownica.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PowderFlaskItem animatable) {
        return null;
    }
}