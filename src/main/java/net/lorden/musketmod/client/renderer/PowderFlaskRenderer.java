package net.lorden.musketmod.client.renderer;

import net.lorden.musketmod.client.model.PowderFlaskModel;
import net.lorden.musketmod.item.PowderFlaskItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PowderFlaskRenderer extends GeoItemRenderer<PowderFlaskItem> {
    public PowderFlaskRenderer() {
        super(new PowderFlaskModel());
    }
}