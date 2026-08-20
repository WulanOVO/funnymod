package yibo.funnymod.client.mixin;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
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
import yibo.funnymod.client.model.HorseElytraModel;
import yibo.funnymod.client.render.FireworkRocketLayer;
import yibo.funnymod.client.render.HorseElytraLayer;

/**
 * 给骷髅马/僵尸马的渲染器附加本模组的渲染层：
 * 烟花火箭渲染层（尾部骨架空腔中渲染火箭），
 * 马背鞘翅渲染层（鞍槽含 GLIDER 组件时在背上渲染鞘翅，机制参考玩家 WingsLayer）。
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
    private void funnymod$addCustomLayers(
        EntityRendererProvider.Context context,
        EquipmentClientInfo.LayerType saddleLayer,
        ModelLayerLocation saddleModel,
        UndeadHorseRenderer.Type adult,
        UndeadHorseRenderer.Type baby,
        CallbackInfo ci
    ) {
        // 复用原版鞘翅模型层（ModelLayers.ELYTRA），但用我们的 HorseElytraModel 包装，
        // 以便从 SaddleElytraRenderState 读取旋转值
        HorseElytraModel elytraModel = new HorseElytraModel(context.bakeLayer(ModelLayers.ELYTRA));
        this.addLayer(new HorseElytraLayer(this, elytraModel));
        this.addLayer(new FireworkRocketLayer(this));
    }
}
