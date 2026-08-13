package yibo.funnymod.client.mixin;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.client.render.FireworkRocketLayer;

/**
 * 给骷髅马/僵尸马的渲染器附加烟花火箭渲染层，在尾部骨架空腔中渲染火箭。
 */
@Mixin(UndeadHorseRenderer.class)
public abstract class UndeadHorseRendererMixin
    extends AbstractHorseRenderer<AbstractHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {

    protected UndeadHorseRendererMixin(
        EntityRendererProvider.Context context,
        AbstractEquineModel<EquineRenderState> model,
        AbstractEquineModel<EquineRenderState> babyModel
    ) {
        super(context, model, babyModel);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void funnymod$addFireworkLayer(
        EntityRendererProvider.Context context,
        EquipmentClientInfo.LayerType saddleLayer,
        ModelLayerLocation saddleModel,
        UndeadHorseRenderer.Type adult,
        UndeadHorseRenderer.Type baby,
        CallbackInfo ci
    ) {
        this.addLayer(new FireworkRocketLayer(this));
    }
}
