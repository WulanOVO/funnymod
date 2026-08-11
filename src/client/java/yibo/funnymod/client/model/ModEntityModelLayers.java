package yibo.funnymod.client.model;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import yibo.funnymod.Funnymod;

public class ModEntityModelLayers {
    public static final ModelLayerLocation SUSPICIOUS_SNOW_GOLEM =
            new ModelLayerLocation(Funnymod.id("suspicious_snow_golem"), "main");

    public static void initialize() {
        ModelLayerRegistry.registerModelLayer(SUSPICIOUS_SNOW_GOLEM,
                SuspiciousSnowGolemModel::createBodyLayer);
    }
}
