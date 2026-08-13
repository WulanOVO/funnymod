package yibo.funnymod.mixin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.entity.FireworkDashHorse;

/**
 * 给骷髅马添加一个只能放烟花火箭的槽位（最多堆叠 64 个），
 * 并支持消耗烟花火箭向前突进（参考鞘翅的烟花火箭伴飞机制）。
 * 突进时长取决于烟花火箭的飞行时长；突进期间撞到生物会对其造成伤害与击退。
 */
@Mixin(SkeletonHorse.class)
public abstract class SkeletonHorseMixin extends AbstractHorse implements FireworkDashHorse {
    /** 突进时的初始水平冲量 */
    @Unique
    private static final double DASH_IMPULSE = 1.0;

    /** 突进时给予的初始向上速度，让骷髅马轻微跃起 */
    @Unique
    private static final double DASH_UP = 0.35;

    /** 突进持续加速度（每 tick，水平方向） */
    @Unique
    private static final double DASH_ACCEL = 0.2;

    /** 突进时长 */
    @Unique
    private static final int DASH_TICKS = 15;

    /** 每个飞行时长额外增加的突进冲量 */
    @Unique
    private static final double DASH_IMPULSE_PER_FLIGHT = 0.2;

    /** 突进撞到生物时对其造成的伤害 */
    @Unique
    private static final float DASH_HIT_DAMAGE = 5.0f;

    /** 突进撞到生物时的击退强度 */
    @Unique
    private static final double DASH_HIT_KNOCKBACK = 1.5;

    /** 突进冲撞检测范围（在马包围盒四周外扩的距离） */
    @Unique
    private static final double DASH_HIT_REACH = 1.0;

    /** 同步「烟花槽是否为空」到客户端的实体数据 key */
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_HAS_FIREWORK =
            SynchedEntityData.defineId(SkeletonHorseMixin.class, EntityDataSerializers.BOOLEAN);

    /** 同步烟花火箭的「飞行时长」到客户端，客户端据此计算突进强度 */
    @Unique
    private static final EntityDataAccessor<Integer> DATA_FIREWORK_FLIGHT_DURATION =
            SynchedEntityData.defineId(SkeletonHorseMixin.class, EntityDataSerializers.INT);

    /** 同步烟花槽中烟花火箭的「数量」到客户端，客户端据此渲染尾部骨架空腔中的火箭 */
    @Unique
    private static final EntityDataAccessor<Integer> DATA_FIREWORK_COUNT =
            SynchedEntityData.defineId(SkeletonHorseMixin.class, EntityDataSerializers.INT);

    /** 烟花火箭槽位（单格）。写入变化时同步 hasFirework 标志与飞行时长到客户端 */
    @Unique
    private final SimpleContainer fireworkContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SkeletonHorseMixin.this.syncFireworkFlag();
        }
    };

    /** 突进剩余 tick 数（仅客户端用于本地模拟突进） */
    @Unique
    private int dashTicks = 0;

    /** 服务端突进（冲撞）结束的 tick 时间戳，用于冲撞判定 */
    @Unique
    private int dashEndTick = 0;

    /** 本次突进已撞过的生物，避免对同一生物反复造成伤害 */
    @Unique
    private final Set<LivingEntity> rammedEntities = new HashSet<>();

    protected SkeletonHorseMixin(EntityType<? extends AbstractHorse> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_HAS_FIREWORK, false);
        entityData.define(DATA_FIREWORK_FLIGHT_DURATION, 0);
        entityData.define(DATA_FIREWORK_COUNT, 0);
    }

    /** 将烟花槽状态（是否为空 + 飞行时长）同步到实体数据 */
    @Unique
    private void syncFireworkFlag() {
        ItemStack stack = this.fireworkContainer.getItem(0);
        this.entityData.set(DATA_HAS_FIREWORK, !stack.isEmpty());
        this.entityData.set(DATA_FIREWORK_COUNT, stack.isEmpty() ? 0 : stack.getCount());

        int flightDuration = 0;
        if (stack.is(Items.FIREWORK_ROCKET)) {
            Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
            if (fireworks != null) {
                flightDuration = fireworks.flightDuration();
            }
        }
        this.entityData.set(DATA_FIREWORK_FLIGHT_DURATION, flightDuration);
    }

    /** 根据烟花火箭的飞行时长计算本次突进的持续 tick 数（客户端与服务端读取同一份同步数据） */
    @Unique
    private double funnymod$getDashImpulse() {
        int flightDuration = this.entityData.get(DATA_FIREWORK_FLIGHT_DURATION);
        return flightDuration > 0 ? flightDuration * DASH_IMPULSE_PER_FLIGHT : DASH_IMPULSE;
    }

    @Override
    public SimpleContainer funnymod$getFireworkContainer() {
        return this.fireworkContainer;
    }

    @Override
    public boolean funnymod$hasFirework() {
        return this.entityData.get(DATA_HAS_FIREWORK);
    }

    @Override
    public int funnymod$getFireworkCount() {
        return this.entityData.get(DATA_FIREWORK_COUNT);
    }

    @Override
    public void funnymod$resetJumpState() {
        // 摆正马身，退出站立/跳跃蓄力状态
        this.clearStanding();
        this.playerJumpPendingScale = 0.0f;
    }

    /**
     * 触发突进。
     * 服务端：消耗烟花火箭 + 生成伴飞烟花实体 + 记录突进状态用于冲撞判定（坐骑移动由客户端权威模拟）。
     * 客户端：设置初始冲量并启动持续加速度，用于本地预测。
     * 突进时长由烟花火箭的飞行时长决定。
     */
    @Override
    public void funnymod$startDash() {
        // 摆正马身、退出跳跃状态
        this.funnymod$resetJumpState();

        if (this.level() instanceof ServerLevel serverLevel) {
            // 服务端：消耗烟花火箭并生成伴飞烟花实体
            ItemStack stack = this.fireworkContainer.getItem(0);
            if (stack.isEmpty() || !stack.is(Items.FIREWORK_ROCKET)) {
                return; // 没有烟花火箭，不突进
            }
            ItemStack consumed = stack.copyWithCount(1);
            stack.shrink(1);
            if (stack.isEmpty()) {
                this.fireworkContainer.setItem(0, ItemStack.EMPTY);
            }
            this.fireworkContainer.setChanged();

            // 参考鞘翅伴飞：不可见烟花实体，跟随骷髅马同步移动，寿命结束爆炸
            FireworkRocketEntity rocket = new FireworkRocketEntity(serverLevel, consumed, this);
            serverLevel.addFreshEntity(rocket);

            this.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);

            // 服务端记录突进状态，用于冲撞判定
            this.rammedEntities.clear();
            this.dashEndTick = this.tickCount + DASH_TICKS;
        } else {
            // 客户端：设置初始冲量并启动持续突进（本地权威模拟）
            Vec3 forward = this.getLookAngle();
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(
                    forward.x * this.funnymod$getDashImpulse(),
                    Math.max(movement.y, DASH_UP),
                    forward.z * this.funnymod$getDashImpulse()
            );
            this.dashTicks = DASH_TICKS;
        }
    }

    /**
     * 覆盖 tickRidden。
     * 客户端：突进期间持续施加水平加速度，模拟鞘翅烟花火箭的持续加速效果。
     * 服务端：突进期间检测冲撞，对撞到的生物造成伤害与击退。
     */
    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        if (this.level().isClientSide()) {
            // 客户端：本地权威模拟持续加速
            if (this.dashTicks > 0) {
                Vec3 forward = this.getLookAngle();
                this.addDeltaMovement(new Vec3(forward.x * DASH_ACCEL, 0.0, forward.z * DASH_ACCEL));
                this.dashTicks--;
            }
        } else if (this.tickCount < this.dashEndTick) {
            // 服务端：冲撞判定（伤害与击退需服务端权威）
            this.applyRammingDamage((ServerLevel) this.level());
        }
    }

    /** 服务端：突进期间对撞到的生物造成伤害与击退 */
    @Unique
    private void applyRammingDamage(ServerLevel level) {
        AABB box = this.getBoundingBox().inflate(DASH_HIT_REACH);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity != this
                        && entity.isAlive()
                        && !this.hasPassenger(entity)
                        && !this.rammedEntities.contains(entity)
        );
        for (LivingEntity target : targets) {
            this.rammedEntities.add(target);
            DamageSource source = this.damageSources().mobAttack(this);
            double dirX = target.getX() - this.getX();
            double dirZ = target.getZ() - this.getZ();
            target.hurtServer(level, source, DASH_HIT_DAMAGE);
            target.knockback(DASH_HIT_KNOCKBACK, dirX, dirZ, source, DASH_HIT_DAMAGE);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSave(ValueOutput output, CallbackInfo ci) {
        output.store("Fireworks", ItemStack.CODEC, this.fireworkContainer.getItem(0));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoad(ValueInput input, CallbackInfo ci) {
        this.fireworkContainer.setItem(0, input.read("Fireworks", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        ItemStack stack = this.fireworkContainer.getItem(0);
        if (!stack.isEmpty()) {
            this.spawnAtLocation(level, stack);
            this.fireworkContainer.setItem(0, ItemStack.EMPTY);
        }
    }
}
