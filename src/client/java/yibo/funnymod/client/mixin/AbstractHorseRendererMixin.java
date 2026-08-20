package yibo.funnymod.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.client.render.FireworkDashHorseRenderState;
import yibo.funnymod.client.render.SaddleElytraRenderState;
import yibo.funnymod.entity.FireworkDashHorse;

/**
 * 在提取坐骑渲染状态时填充本模组附加数据：
 * 烟花火箭槽的 item 渲染状态与数量，以及鞘翅旋转值（复刻玩家鞘翅机制，
 * 从 {@code LivingEntity#elytraAnimationState} 提取）。
 */
@Mixin(AbstractHorseRenderer.class)
public abstract class AbstractHorseRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void funnymod$extractExtraRenderState(
        AbstractHorse entity, EquineRenderState state, float partialTicks, CallbackInfo ci) {
        SaddleElytraRenderState elytraAccess = (SaddleElytraRenderState) (Object) state;
        // 复刻玩家鞘翅：从实体 elytraAnimationState 取插值后的旋转角，供 HorseElytraModel 读取
        elytraAccess.funnymod$setElytraRotX(entity.elytraAnimationState.getRotX(partialTicks));
        elytraAccess.funnymod$setElytraRotY(entity.elytraAnimationState.getRotY(partialTicks));
        elytraAccess.funnymod$setElytraRotZ(entity.elytraAnimationState.getRotZ(partialTicks));

        FireworkDashHorseRenderState access = (FireworkDashHorseRenderState) (Object) state;
        if (entity instanceof FireworkDashHorse horse) {
            int count = horse.funnymod$getFireworkCount();
            access.funnymod$setFireworkCount(count);
            if (count > 0) {
                Minecraft.getInstance().getItemModelResolver().updateForNonLiving(
                    access.funnymod$getFireworkItem(),
                    new ItemStack(Items.FIREWORK_ROCKET),
                    ItemDisplayContext.FIXED,
                    entity
                );
            } else {
                access.funnymod$getFireworkItem().clear();
            }
        } else {
            access.funnymod$setFireworkCount(0);
            access.funnymod$getFireworkItem().clear();
        }
    }
}
