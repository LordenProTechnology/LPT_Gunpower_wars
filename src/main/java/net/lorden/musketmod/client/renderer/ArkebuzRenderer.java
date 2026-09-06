package net.lorden.musketmod.client.renderer;

import net.lorden.musketmod.client.model.ArkebuzModel;
import net.lorden.musketmod.item.ArkebuzItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ArkebuzRenderer extends GeoItemRenderer<ArkebuzItem> {
    public ArkebuzRenderer() {
        super(new ArkebuzModel());
    }
}