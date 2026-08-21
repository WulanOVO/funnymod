package yibo.funnymod.mixin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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

    /** 骷髅马始终解锁的右侧物品格列数（2列×3行=6格） */
    @Unique
    private static final int INVENTORY_COLUMNS = 2;

    /** 弩射击箭矢威力（与原版弩 ARROW_POWER 一致） */
    @Unique
    private static final float ARROW_POWER = 3.15f;

    /** 弩射击最小间隔（tick），无视快速装填 */
    @Unique
    private static final int SHOOT_COOLDOWN_TICKS = 4;

    /** 弩箭目标搜索范围（格） */
    @Unique
    private static final double TARGET_SEARCH_RANGE = 48.0;

    /** 弩箭瞄准圆锥半角（度） */
    @Unique
    private static final double AIM_CONE_HALF_ANGLE = 30.0;

    /** 弩箭弹道重力补偿系数（参考骷髅的 0.2） */
    @Unique
    private static final float GRAVITY_COMPENSATION = 0.10f;

    /** 上次射击时的 tickCount，用于计算冷却 */
    @Unique
    private int lastShootTick = -SHOOT_COOLDOWN_TICKS;

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

    /** 同步「弩槽是否为空」到客户端的实体数据 key */
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_HAS_CROSSBOW =
            SynchedEntityData.defineId(SkeletonHorseMixin.class, EntityDataSerializers.BOOLEAN);

    /** 同步弩槽中的实际 ItemStack 到客户端，客户端据此渲染装填状态、附魔光泽、耐久等 */
    @Unique
    private static final EntityDataAccessor<ItemStack> DATA_CROSSBOW_STACK =
            SynchedEntityData.defineId(SkeletonHorseMixin.class, EntityDataSerializers.ITEM_STACK);

    /** 烟花火箭槽位（单格）。写入变化时同步 hasFirework 标志与飞行时长到客户端 */
    @Unique
    private final SimpleContainer fireworkContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SkeletonHorseMixin.this.syncFireworkFlag();
        }
    };

    /** 弩槽位（单格，不可堆叠）。写入变化时同步 hasCrossbow 标志到客户端 */
    @Unique
    private final SimpleContainer crossbowContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SkeletonHorseMixin.this.syncCrossbowFlag();
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
        entityData.define(DATA_HAS_CROSSBOW, false);
        entityData.define(DATA_CROSSBOW_STACK, ItemStack.EMPTY);
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

    /** 将弩槽状态（是否为空 + 实际 ItemStack）同步到实体数据 */
    @Unique
    private void syncCrossbowFlag() {
        ItemStack stack = this.crossbowContainer.getItem(0);
        this.entityData.set(DATA_HAS_CROSSBOW, !stack.isEmpty());
        this.entityData.set(DATA_CROSSBOW_STACK, stack.copy());
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

    /** 始终解锁右侧 2 列（6 格）物品栏，无需装备箱子 */
    @Override
    public int getInventoryColumns() {
        return INVENTORY_COLUMNS;
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
    public SimpleContainer funnymod$getCrossbowContainer() {
        return this.crossbowContainer;
    }

    @Override
    public boolean funnymod$hasCrossbow() {
        return this.entityData.get(DATA_HAS_CROSSBOW);
    }

    @Override
    public ItemStack funnymod$getCrossbowStack() {
        return this.entityData.get(DATA_CROSSBOW_STACK);
    }

    @Override
    public void funnymod$resetJumpState() {
        // 摆正马身，退出站立/跳跃蓄力状态
        this.clearStanding();
        this.playerJumpPendingScale = 0.0f;
    }

    /**
     * 装备弩时，以玩家视角方向发射箭矢，消耗马物品栏中的箭。
     * 自动瞄准 30° 圆锥内距离主轴最近的生物并做粗略抛物线预测（参考骷髅），
     * 无目标时向正前方发射。多重射击散射与弩附魔叠加均自动处理。
     */
    @Override
    public void funnymod$shootCrossbow(Player player) {
        if (this.level().isClientSide()) return;
        if (this.tickCount - this.lastShootTick < SHOOT_COOLDOWN_TICKS) return;

        ItemStack crossbow = this.crossbowContainer.getItem(0);
        if (!crossbow.is(Items.CROSSBOW)) return;

        // 在马物品栏中查找箭矢
        ItemStack arrowStack = ItemStack.EMPTY;
        int arrowSlot = -1;
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (stack.is(ItemTags.ARROWS)) {
                arrowStack = stack;
                arrowSlot = i;
                break;
            }
        }
        if (arrowStack.isEmpty()) return;

        ServerLevel serverLevel = (ServerLevel) this.level();

        // 在 30° 圆锥范围内搜索距离主轴最近的生物
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 horsePos = this.position();
        AABB searchBox = AABB.ofSize(horsePos, TARGET_SEARCH_RANGE * 2, TARGET_SEARCH_RANGE * 2, TARGET_SEARCH_RANGE * 2);
        LivingEntity bestTarget = null;
        double bestAngle = AIM_CONE_HALF_ANGLE;

        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (entity == this || entity == player || !entity.isAlive()) continue;
            Vec3 toEntity = entity.position().subtract(horsePos);
            double distance = toEntity.length();
            if (distance > TARGET_SEARCH_RANGE || distance < 0.5) continue;
            toEntity = toEntity.scale(1.0 / distance);
            double dot = Math.max(-1.0, Math.min(1.0, lookVec.dot(toEntity)));
            double angle = Math.toDegrees(Math.acos(dot));
            // 视线检测：剔除被方块/流体遮挡的目标，避免瞄准墙壁后的实体
            if (angle <= bestAngle && this.hasLineOfSight(entity)) {
                bestAngle = angle;
                bestTarget = entity;
            }
        }

        // 计算基础发射方向
        Vector3f baseDir;
        if (bestTarget != null) {
            // 目标锁定：参考骷髅弹道补偿（yd + 距离 * 补偿系数）
            double xd = bestTarget.getX() - this.getX();
            double yd = bestTarget.getY(0.3333333333333333) - (this.getEyeY() - 0.1);
            double zd = bestTarget.getZ() - this.getZ();
            double distanceToTarget = Math.sqrt(xd * xd + zd * zd);
            baseDir = new Vector3f(
                    (float) xd,
                    (float) (yd + distanceToTarget * GRAVITY_COMPENSATION),
                    (float) zd);
        } else {
            // 无目标：向正前方发射
            baseDir = lookVec.toVector3f();
        }

        // 多重射击：弹射物数量与散射角度（复刻 ProjectileWeaponItem.shoot）
        int projectileCount = EnchantmentHelper.processProjectileCount(serverLevel, crossbow, this, 1);
        float maxSpread = EnchantmentHelper.processProjectileSpread(serverLevel, crossbow, this, 0.0f);
        float angleStep = projectileCount == 1 ? 0.0f : 2.0f * maxSpread / (projectileCount - 1);
        float angleOffset = ((projectileCount - 1) % 2) * angleStep / 2.0f;
        float direction = 1.0f;

        Vec3 upVec = player.getUpVector(1.0f);

        for (int i = 0; i < projectileCount; i++) {
            float angle = angleOffset + direction * ((i + 1) / 2) * angleStep;
            direction = -direction;

            // 围绕 up 向量旋转基础方向，产生多重射击散射
            Quaternionf rotQuat = new Quaternionf().setAngleAxis(
                    angle * (Math.PI / 180.0), upVec.x, upVec.y, upVec.z);
            Vector3f shotVec = new Vector3f(baseDir).rotate(rotQuat);

            // 使用原版 ArrowItem.createArrow 创建箭矢（正确处理药箭、光灵箭等）
            ItemStack arrowCopy = arrowStack.copyWithCount(1);
            Item item = arrowCopy.getItem();
            ArrowItem arrowItem = (item instanceof ArrowItem arrowI) ? arrowI : (ArrowItem) Items.ARROW;
            AbstractArrow arrow = arrowItem.createArrow(serverLevel, arrowCopy, this, crossbow);
            arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;

            // 发射并应用附魔（applyOnProjectileSpawned 自动处理穿透、火焰等）
            Projectile.spawnProjectile(arrow, serverLevel, arrowCopy, a ->
                    a.shoot(shotVec.x(), shotVec.y(), shotVec.z(), ARROW_POWER, 1.0f));
        }

        // 消耗一支箭（多重射击只消耗 1 发弹药）
        arrowStack.shrink(1);
        if (arrowStack.isEmpty()) {
            this.inventory.setItem(arrowSlot, ItemStack.EMPTY);
        } else {
            this.inventory.setChanged();
        }

        this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f, 1.0f);
        this.lastShootTick = this.tickCount;
    }

    /**
     * 触发突进。
     * 服务端：消耗烟花火箭 + 记录突进状态用于冲撞判定（坐骑移动由客户端权威模拟）。
     * 客户端：设置初始冲量并启动持续加速度，用于本地预测。
     * 突进时长由烟花火箭的飞行时长决定。
     */
    @Override
    public void funnymod$startDash() {
        // 摆正马身、退出跳跃状态
        this.funnymod$resetJumpState();

        if (this.level() instanceof ServerLevel serverLevel) {
            // 服务端：消耗烟花火箭
            ItemStack stack = this.fireworkContainer.getItem(0);
            if (stack.isEmpty() || !stack.is(Items.FIREWORK_ROCKET)) {
                return; // 没有烟花火箭，不突进
            }
            // 先复制一份用于生成伴飞烟花火箭（之后再消耗）
            ItemStack fireworkCopy = stack.copy();
            stack.shrink(1);
            if (stack.isEmpty()) {
                this.fireworkContainer.setItem(0, ItemStack.EMPTY);
            }
            this.fireworkContainer.setChanged();

            this.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);

            // 生成附着在马身上的烟花火箭实体，提供粒子、音效、爆炸等视觉效果。
            // 加速由 FireworkRocketEntityMixin 对非玩家实体跳过，不影响飞行状态。
            FireworkRocketEntity rocket = new FireworkRocketEntity(serverLevel, fireworkCopy, this);
            serverLevel.addFreshEntity(rocket);

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
     * 鞍槽是否装备了鞍鞘（含 GLIDER 组件的鞍槽物品）。
     */
    @Unique
    private boolean funnymod$hasSaddleElytra() {
        ItemStack saddleSlot = this.getItemBySlot(EquipmentSlot.SADDLE);
        return !saddleSlot.isEmpty() && saddleSlot.has(DataComponents.GLIDER);
    }

    /**
     * 覆盖 tickRidden。
     * 客户端：突进期间持续加速。
     *   - 装备鞍鞘：复刻 {@link FireworkRocketEntity#tick()} 的加速公式
     *     {@code new_delta = 0.5 * old + 0.85 * lookAngle}，
     *     速度平滑收敛到 {@code 1.5 * lookAngle}，与玩家鞘翅+烟花火箭的飞行手感一致。
     *     同时突进期间自动保持滑翔（setSharedFlag(7,true)），确保走 travelFallFlying。
     *   - 未装备鞍鞘：走原始的恒定加速度突进。
     * 服务端：突进期间检测冲撞，对撞到的生物造成伤害与击退。
     */
    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        if (this.level().isClientSide()) {
            if (this.dashTicks > 0) {
                if (this.funnymod$hasSaddleElytra()) {
                    // 突进期间若装备鞍鞘且已离地，自动保持滑翔状态，
                    // 使 travel() 走 travelFallFlying 而非 travelInAir
                    if (!this.onGround() && !this.isFallFlying()) {
                        this.setSharedFlag(7, true);
                    }
                    // 复刻 FireworkRocketEntity.tick() 对附着实体的加速：
                    // movement += look * 0.1 + (look * 1.5 - movement) * 0.5
                    // 化简 = 0.5 * old + 0.85 * look（目标速度 1.5 * lookAngle 的指数平滑收敛）
                    Vec3 look = this.getLookAngle();
                    Vec3 movement = this.getDeltaMovement();
                    this.setDeltaMovement(movement.add(
                            look.x * 0.1 + (look.x * 1.5 - movement.x) * 0.5,
                            look.y * 0.1 + (look.y * 1.5 - movement.y) * 0.5,
                            look.z * 0.1 + (look.z * 1.5 - movement.z) * 0.5));
                } else {
                    Vec3 forward = this.getLookAngle();
                    this.addDeltaMovement(new Vec3(forward.x * DASH_ACCEL, 0.0, forward.z * DASH_ACCEL));
                }
                this.dashTicks--;
            }
        } else {
            // 服务端：突进期间若装备鞍鞘且离地，也保持滑翔状态（与客户端同步），
            // 使 isFallFlying() 在服务端为 true，用于进度检测等服务端逻辑
            if (this.tickCount < this.dashEndTick
                    && this.funnymod$hasSaddleElytra()
                    && !this.onGround()
                    && !this.isFallFlying()) {
                this.setSharedFlag(7, true);
            }
            // 服务端：冲撞判定（伤害与击退需服务端权威）
            if (this.tickCount < this.dashEndTick) {
                this.applyRammingDamage((ServerLevel) this.level());
            }
        }
    }

    /**
     * 当前是否处于突进飞行状态。
     * 客户端用本地 dashTicks 判定，服务端用 dashEndTick 时间戳判定。
     */
    @Unique
    private boolean funnymod$isDashing() {
        return this.level().isClientSide() ? this.dashTicks > 0 : this.tickCount < this.dashEndTick;
    }

    /**
     * 突进飞行期间不累积摔落距离，避免持续用烟花火箭保持腾空时，
     * fallDistance 只增不减（上升不减少）导致落地莫名摔死。
     */
    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (this.funnymod$isDashing()) {
            this.resetFallDistance();
        }
        super.checkFallDamage(ya, onGround, onState, pos);
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
        output.store("Crossbow", ItemStack.CODEC, this.crossbowContainer.getItem(0));

        // 持久化右侧物品格内容（参考 AbstractChestedHorse）
        ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("Items", ItemStackWithSlot.CODEC);
        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                items.add(new ItemStackWithSlot(i, stack));
            }
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoad(ValueInput input, CallbackInfo ci) {
        this.fireworkContainer.setItem(0, input.read("Fireworks", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.crossbowContainer.setItem(0, input.read("Crossbow", ItemStack.CODEC).orElse(ItemStack.EMPTY));

        // 加载右侧物品格内容（构造时 createInventory 已创建 6 格容器）
        for (ItemStackWithSlot item : input.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            if (item.isValidInContainer(this.inventory.getContainerSize())) {
                this.inventory.setItem(item.slot(), item.stack());
            }
        }
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        ItemStack firework = this.fireworkContainer.getItem(0);
        if (!firework.isEmpty()) {
            this.spawnAtLocation(level, firework);
            this.fireworkContainer.setItem(0, ItemStack.EMPTY);
        }
        ItemStack crossbow = this.crossbowContainer.getItem(0);
        if (!crossbow.isEmpty()) {
            this.spawnAtLocation(level, crossbow);
            this.crossbowContainer.setItem(0, ItemStack.EMPTY);
        }
    }
}
