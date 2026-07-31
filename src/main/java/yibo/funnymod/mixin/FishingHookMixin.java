package yibo.funnymod.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
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
import yibo.funnymod.Funnymod;

/**
 * 拽引附魔的完整实现：
 *
 * 问题根因：
 * 1. Entity.move() 中 onGround 每 2 tick 翻转（delta.y=0 时 onGround 被设为 false）
 * 2. onGround=false 时重力行会执行，钩子每 tick 下滑 0.03 格
 * 3. retrieve() 检查 onGround/horizontalCollision 不可靠
 *
 * 解决方案：
 * - HEAD 注入：钩子已粘住时，设 onGround=true 跳过重力 + 锁定位置 + 清零速度
 *   （客户端和服务端都执行，否则客户端独立加重力导致渲染下滑/回弹）
 * - TAIL 注入：检测钩子是否已停止（连续 2 tick 速度≈0），标记为粘住
 *   （客户端和服务端都执行，保证双方 stickPos / isStuck 一致）
 * - retrieve()：只检查自己的 isStuck 标志，完全不依赖 MC 的 onGround
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow
    private Entity hookedIn;

    /** 钩子粘住时的锁定位置 */
    @Unique
    private Vec3 funnymod$stickPos;

    /** 连续静止的 tick 计数 */
    @Unique
    private int funnymod$stillCount;

    /** 钩子是否已粘在方块上 */
    @Unique
    private boolean funnymod$isStuck;

    // ==================== 工具方法 ====================

    /**
     * 遍历物品附魔，按 ID 找拽引等级
     */
    private static int findGrappleLevel(ItemStack stack) {
        if (!stack.is(Items.FISHING_ROD)) return 0;
        var enchants = stack.getEnchantments();
        if (enchants.isEmpty()) return 0;
        Identifier id = Funnymod.id("grapple");
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(id)) return entry.getIntValue();
        }
        return 0;
    }

    /** 检查玩家主/副手钓竿是否有拽引附魔 */
    private static int getOwnerGrappleLevel(Player owner) {
        int level = findGrappleLevel(owner.getMainHandItem());
        if (level <= 0) level = findGrappleLevel(owner.getOffhandItem());
        return level;
    }

    /**
     * 锁定已粘住的钩子
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void grappleLockStuck(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        // 客户端和服务端都要执行，否则客户端会独立加重力导致渲染下滑/回弹
        if (!funnymod$isStuck || funnymod$stickPos == null) return;

        Player owner = self.getPlayerOwner();
        if (owner == null) return;
        if (getOwnerGrappleLevel(owner) <= 0) return;

        // ★ 关键：设置 onGround=true，让第 229 行的重力检查 !onGround() 为 false，跳过重力
        self.setOnGround(true);
        // 锁定位置到粘住点
        self.setPos(funnymod$stickPos.x, funnymod$stickPos.y, funnymod$stickPos.z);
        // 清零速度，move(ZERO) 不会移动实体
        self.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * 检测钩子是否已停止
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void grappleDetectStuck(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;

        // 在水中 → 正常钓鱼，不处理
        if (self.isInWater()) {
            funnymod$isStuck = false;
            funnymod$stickPos = null;
            funnymod$stillCount = 0;
            return;
        }

        Player owner = self.getPlayerOwner();
        if (owner == null) return;
        if (getOwnerGrappleLevel(owner) <= 0) {
            funnymod$isStuck = false;
            funnymod$stickPos = null;
            funnymod$stillCount = 0;
            return;
        }

        // 检查钩子是否已停止运动
        double velSq = self.getDeltaMovement().lengthSqr();
        if (velSq < 0.001) {
            funnymod$stillCount++;
            if (funnymod$stillCount >= 2) {
                // 连续 2 tick 速度≈0 → 钩子已粘住
                if (funnymod$stickPos == null) {
                    funnymod$stickPos = self.position();
                }
                funnymod$isStuck = true;
                self.setDeltaMovement(Vec3.ZERO);
            }
        } else {
            // 还在运动中
            funnymod$stillCount = 0;
            funnymod$isStuck = false;
            funnymod$stickPos = null;
        }
    }

    /**
     * 执行拽引效果：
     * - 钩中方块：玩家拉向钩子位置
     * - 钩中生物：玩家和生物互相拉向对方，中间撞到一起
     */
    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void grappleRetrieve(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;

        if (self.isInWater()) return;

        Player owner = self.getPlayerOwner();
        if (owner == null) return;

        int level = findGrappleLevel(rod);
        if (level <= 0) return;

        double strength = 1.0 + 0.5 * level; // Lv1=1.5, Lv2=2.0, Lv3=2.5

        // 情况 1：钩中了生物 → 玩家和生物互相拉向对方
        if (this.hookedIn != null) {
            Entity target = this.hookedIn;
            Vec3 playerPos = owner.position();
            Vec3 targetPos = target.position();

            // 玩家 → 拉向生物
            Vec3 dir = new Vec3(
                    targetPos.x - playerPos.x,
                    targetPos.y - playerPos.y,
                    targetPos.z - playerPos.z
            ).normalize();
            owner.setDeltaMovement(dir.multiply(strength, strength, strength));
            owner.hurtMarked = true;
            owner.fallDistance = 0;

            // 生物 → 拉向玩家（额外施加，叠加原版 pullEntity 效果）
            Vec3 dirToPlayer = dir.reverse();
            target.setDeltaMovement(target.getDeltaMovement().add(
                    dirToPlayer.scale(strength * 0.6)));
            target.hurtMarked = true;
            target.fallDistance = 0;

            self.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS,
                    1.0f, 0.5f);

            // 不 cancel，让原版 retrieve() 继续处理 pullEntity 和伤害
            return;
        }

        // 情况 2：钩中方块 → 玩家拉向钩子位置
        if (!funnymod$isStuck) return;

        Vec3 hookPos = self.position();
        Vec3 playerPos = owner.position();
        Vec3 dir = new Vec3(
                hookPos.x - playerPos.x,
                hookPos.y - playerPos.y,
                hookPos.z - playerPos.z
        ).normalize();

        owner.setDeltaMovement(dir.multiply(strength, strength, strength));
        owner.hurtMarked = true;
        owner.fallDistance = 0;

        self.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS,
                1.0f, 0.5f);

        self.discard();

        // 消耗 3 点耐久值
        cir.setReturnValue(3);
    }
}
