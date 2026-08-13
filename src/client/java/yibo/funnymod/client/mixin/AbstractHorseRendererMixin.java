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
import yibo.funnymod.entity.FireworkDashHorse;

/**
 * 在提取坐骑渲染状态时，把骷髅马烟花槽中的火箭数量与火箭 item 渲染状态填入渲染状态。
 * 火箭渲染用 item display 实体的方式（ItemModelResolver + FIXED 展示上下文）。
 */
@Mixin(AbstractHorseRenderer.class)
public abstract class AbstractHorseRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void funnymod$extractFirework(
        AbstractHorse entity, EquineRenderState state, float partialTicks, CallbackInfo ci) {
        FireworkDashHorseRenderState access = (FireworkDashHorseRenderState) (Object) state;
        if (entity instanceof FireworkDashHorse horse) {
            int count = horse.funnymod$getFireworkCount();
            access.funnymod$setFireworkCount(count);
            if (count > 0) {
                // 烟花火箭外观固定，用一个新的 ItemStack 渲染即可（数量由 fireworkCount 决定）
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
