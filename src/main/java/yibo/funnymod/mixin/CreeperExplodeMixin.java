package yibo.funnymod.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.entity.SuspiciousSnowGolemEntity;

@Mixin(Creeper.class)
public class CreeperExplodeMixin {

    /**
     * 可疑雪傀儡爆炸前释放背包中所有雪球。
     */
    @Inject(method = "explodeCreeper", at = @At("HEAD"))
    private void onExplodeCreeper(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        if (self instanceof SuspiciousSnowGolemEntity ssg) {
            ssg.releaseSnowballs();
        }
    }
}
