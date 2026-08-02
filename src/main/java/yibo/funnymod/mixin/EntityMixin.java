package yibo.funnymod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.effect.ModEffects;

/**
 * 在胶着或粘脚状态下走路时，额外播放蜂蜜块脚步声
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "playStepSound", at = @At("TAIL"))
    private void playHoneyStepSound(BlockPos pos, BlockState blockState, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity living)) return;

        boolean hasSticky = living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.STICKY));
        boolean hasStickyFeet = living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.STICKY_FEET));
        if (!hasSticky && !hasStickyFeet) return;

        SoundType honeySound = SoundType.HONEY_BLOCK;
        living.playSound(
                honeySound.getStepSound(),
                honeySound.getVolume() * 0.15f,
                honeySound.getPitch()
        );
    }
}
