package yibo.funnymod.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yibo.funnymod.effect.ModEffects;
import yibo.funnymod.entity.GlidePitchData;
import yibo.funnymod.entity.RiderJumpInput;
import yibo.funnymod.util.GlidePitchUtil;

/**
 * 粘脚效果：模拟蜂蜜块，削减跳跃高度（跳一半）让玩家跳不了一格
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements GlidePitchData {
    @Shadow
    protected abstract boolean canGlide();

    @Shadow
    public abstract boolean isFallFlying();

    @Unique
    private float funnymod$glidePitch;
    @Unique
    private float funnymod$glidePitchO;
    @Unique
    private float funnymod$glidePitchPeak;

    @Override
    public float funnymod$getGlidePitch() {
        return funnymod$glidePitch;
    }

    @Override
    public void funnymod$setGlidePitch(float pitch) {
        funnymod$glidePitch = pitch;
    }

    @Override
    public float funnymod$getGlidePitchO() {
        return funnymod$glidePitchO;
    }

    @Override
    public void funnymod$setGlidePitchO(float pitch) {
        funnymod$glidePitchO = pitch;
    }

    @Override
    public float funnymod$getGlidePitchPeak() {
        return funnymod$glidePitchPeak;
    }

    @Override
    public void funnymod$setGlidePitchPeak(float pitch) {
        funnymod$glidePitchPeak = pitch;
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void reduceStickyFeetJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.STICKY_FEET))) {
            Vec3 movement = self.getDeltaMovement();
            self.setDeltaMovement(movement.x, movement.y * 0.5, movement.z);
            self.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 0.5F, 1.0F);
        }
    }

    /**
     * 坐骑滑翔：被玩家骑乘的坐骑装着鞍鞘（GLIDER 组件在鞍槽）时，
     * 在空中按住跳跃键即启动滑翔（对应共享标志位 7，与玩家鞘翅同一状态）。
     * 启动后滑翔物理由原版 {@code travelFallFlying} 自动接管，
     * 停止条件（触地/入水/耐久耗尽等）由原版 {@code updateFallFlying} 每 tick 检查。
     * 客户端（移动权威）与服务端（状态同步/伤害判定）各自独立触发，无需额外网络包。
     */
    @Inject(method = "tickRidden", at = @At("TAIL"))
    private void funnymod$tryStartGlideWhenRidden(Player controller, Vec3 riddenInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isFallFlying() || self.onGround() || self.isInWater()) {
            return;
        }
        // 仅响应实现了按键状态接口的真实玩家（LocalPlayer / ServerPlayer）
        if (!(controller instanceof RiderJumpInput jumpInput) || !jumpInput.funnymod$isJumpHeld()) {
            return;
        }
        if (this.canGlide()) {
            // setSharedFlag 声明在 Entity（protected），已通过 access widener 开放，直接调用
            self.setSharedFlag(7, true);
        }
    }

    /**
     * 客户端安全网：落地后若滑翔标志位 7 仍为 true（与服务端 {@code updateFallFlying}
     * 同步延迟或丢失），强制清除，避免鞘翅渲染卡在展开态、移动走滑翔物理导致方向键失控。
     * 触地 + isFallFlying 是不一致状态，落地本不该继续滑翔。
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void funnymod$forceClearGlideOnGround(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() && self.onGround() && self.isFallFlying()) {
            self.setSharedFlag(7, false);
        }
    }

    /**
     * 每 tick 更新平滑滑翔俯仰：起飞/触地时俯仰在几 tick 内过渡，
     * 渲染端与相机用 partialTicks 在 O/当前值间插值，避免视角突变。
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void funnymod$smoothGlidePitch(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isFallFlying() || funnymod$glidePitch != 0.0f || funnymod$glidePitchO != 0.0f) {
            GlidePitchUtil.tickGlidePitch(self);
        }
    }

    /**
     * 滑翔俯仰座位：鞍鞘坐骑滑翔时，骑手座位随渲染俯仰一起绕躯干中心旋转，
     * 骑手贴住鞍座。双端生效（客户端本地玩家用精确视角，服务端用同步视角）。
     */
    @Inject(method = "getPassengerRidingPosition", at = @At("RETURN"), cancellable = true)
    private void funnymod$rotateGlideSeat(Entity passenger, CallbackInfoReturnable<Vec3> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!GlidePitchUtil.shouldApplyGlideRotation(self)) {
            return;
        }
        float pitch = GlidePitchUtil.computeGlidePitch(self, 0.0f);
        if (GlidePitchUtil.isNegligible(pitch)) {
            return;
        }
        cir.setReturnValue(GlidePitchUtil.rotateSeat(cir.getReturnValue(), self, pitch));
    }

    /**
     * 鞍鞘耐久到 maxDamage-1（原版 nextDamageWillBreak 状态，失去滑翔资格）时，
     * 把鞍鞘拆成破损鞘翅掉落物 + 普通马鞍，保留骑乘能力、失去滑翔能力。
     */
    @Inject(method = "updateFallFlying", at = @At("HEAD"))
    private void funnymod$breakSaddleElytraOnLowDurability(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        ItemStack saddleSlot = self.getItemBySlot(EquipmentSlot.SADDLE);
        if (saddleSlot.isEmpty() || !saddleSlot.has(DataComponents.GLIDER) || !saddleSlot.nextDamageWillBreak()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) self.level();
        ItemStack brokenElytra = new ItemStack(Items.ELYTRA);
        brokenElytra.setDamageValue(brokenElytra.getMaxDamage());
        self.spawnAtLocation(serverLevel, brokenElytra);
        // 鞍槽破碎视觉事件（事件码 68 = SADDLE）
        serverLevel.broadcastEntityEvent(self, (byte) 68);
        self.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
    }
}
