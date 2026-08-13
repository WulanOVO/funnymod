package yibo.funnymod.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.entity.FireworkDashHorse;

/**
 * 客户端：在骷髅马的库存界面，于马鞍槽位下方渲染烟花火箭槽位背景。
 * mixin 到 AbstractMountInventoryScreen（mount 与 extractBackground 均定义于此），
 * 对 HorseInventoryScreen 与其它坐骑库存界面同时生效，仅在坐骑为骷髅马时渲染。
 */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class AbstractMountInventoryScreenMixin {
    @Shadow
    protected LivingEntity mount;

    /** 与 HorseInventoryScreen#getSlotSpriteLocation 相同的默认槽位贴图 */
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void renderFireworkSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!(this.mount instanceof FireworkDashHorse)) return;

        // 马甲槽背景位置 (leftPos+7, topPos+35)，即马鞍槽 (leftPos+7, topPos+17) 的正下方
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, screen.leftPos + 7, screen.topPos + 35, 18, 18);
    }
}
