package yibo.funnymod.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.Funnymod;
import yibo.funnymod.component.ModDataComponents;
import yibo.funnymod.entity.MixedSnowballEntity;
import yibo.funnymod.entity.SuspiciousSnowGolemEntity;
import yibo.funnymod.entity.goal.SnowGolemPickUpGoal;
import yibo.funnymod.item.ModItems;

import java.util.ArrayList;
import java.util.List;

@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin extends Mob {
    @Unique
    private static final TagKey<Item> SNOWBALL_ADDITIVES =
            TagKey.create(Registries.ITEM, Funnymod.id("snowball_additives"));

    @Unique
    private static final int MAX_SNOWBALLS = 64;
    @Unique
    private static final int SNOWBALLS_PER_HEALTH = 16;
    @Unique
    private static final int HEAL_INTERVAL = 10; // 0.5 seconds = 10 ticks
    @Unique
    private static final float BASE_HEALTH = 4.0f;

    @Unique
    private final SimpleContainer additiveInventory = new SimpleContainer(5);

    @Unique
    private int snowballCount = 0;

    @Unique
    private int healCooldown = 0;

    protected SnowGolemMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.setCanPickUpLoot(true);
    }

    /**
     * 雪球存到专用计数器（每16个+1血量上限），添加剂存到5格库存。
     * 火药必须有烈焰粉配合才有效果，库存无烈焰粉时拒捡。
     */
    @Override
    public boolean wantsToPickUp(@NonNull ServerLevel level, ItemStack itemStack) {
        if (itemStack.is(Items.SNOWBALL)) return snowballCount < MAX_SNOWBALLS;
        if (!itemStack.is(SNOWBALL_ADDITIVES)) return false;
        return !itemStack.is(Items.GUNPOWDER) || hasBlazePowder();
    }

    /**
     * 雪球 → 计数器（每16个+1血量上限）；添加剂 → 优先填空槽位（不合并），槽满后才补充同类型槽位。
     */
    @Override
    protected void pickUpItem(@NonNull ServerLevel level, ItemEntity entity) {
        ItemStack itemStack = entity.getItem();
        if (itemStack.isEmpty()) return;

        // 雪球走专用计数器
        if (itemStack.is(Items.SNOWBALL)) {
            int canTake = Math.min(itemStack.getCount(), MAX_SNOWBALLS - snowballCount);
            if (canTake <= 0) return;
            int oldCount = snowballCount;
            snowballCount += canTake;
            itemStack.shrink(canTake);
            onSnowballCountChanged(oldCount);
            this.onItemPickup(entity);
            this.take(entity, canTake);
            if (itemStack.isEmpty()) entity.discard();
            return;
        }

        if (!itemStack.is(SNOWBALL_ADDITIVES)) return;

        // 优先放入空槽位（即使已有同类型物品也另起新槽）
        int emptySlot = -1;
        for (int i = 0; i < 5; i++) {
            if (additiveInventory.getItem(i).isEmpty()) {
                emptySlot = i;
                break;
            }
        }

        if (emptySlot >= 0) {
            ItemStack taken = itemStack.split(itemStack.getCount());
            additiveInventory.setItem(emptySlot, taken);
            this.onItemPickup(entity);
            this.take(entity, taken.getCount());
            if (itemStack.isEmpty()) {
                entity.discard();
            }
            return;
        }

        // 槽位全满，尝试补充已有同类型槽位（补货）
        for (int i = 0; i < 5; i++) {
            ItemStack existing = additiveInventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, itemStack)) {
                int room = existing.getMaxStackSize() - existing.getCount();
                if (room > 0) {
                    int toAdd = Math.min(room, itemStack.getCount());
                    existing.grow(toAdd);
                    itemStack.shrink(toAdd);
                    this.onItemPickup(entity);
                    this.take(entity, toAdd);
                    if (itemStack.isEmpty()) {
                        entity.discard();
                    }
                    return;
                }
            }
        }
        // 全满且无同类型可补 — 不拾取
    }

    /**
     * 死亡时掉落库存中所有物品，不掉落雪球。
     */
    @Override
    protected void dropEquipment(@NonNull ServerLevel level) {
        super.dropEquipment(level);
        for (int i = 0; i < 5; i++) {
            ItemStack stack = additiveInventory.getItem(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(level, stack);
                additiveInventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 回血：每0.5秒消耗1个雪球恢复1点生命值，直到满血。
     */
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel)) return;

        // 如果目标变成戴上南瓜的可疑雪傀儡，立即失去仇恨
        if (this.getTarget() instanceof SuspiciousSnowGolemEntity ssg && ssg.hasPumpkin()) {
            this.setTarget(null);
        }

        if (snowballCount <= 0 || this.getHealth() >= this.getMaxHealth()) return;

        if (healCooldown > 0) {
            healCooldown--;
            return;
        }

        healCooldown = HEAL_INTERVAL;
        int oldCount = snowballCount;
        snowballCount--;
        this.heal(1.0f);
        onSnowballCountChanged(oldCount);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegisterGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(2, new SnowGolemPickUpGoal(this, 1.0, 10.0));
    }

    /**
     * 发射雪球时从5个槽位各消耗1个物品，揉入混合雪球。
     * 若库存为空则回退到原版普通雪球。
     */
    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void onPerformRangedAttack(LivingEntity target, float power, CallbackInfo ci) {
        // 不攻击戴上南瓜的可疑雪傀儡
        if (target instanceof SuspiciousSnowGolemEntity ssg && ssg.hasPumpkin()) {
            ci.cancel();
            return;
        }

        List<Holder<Item>> consumed = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = additiveInventory.getItem(i);
            if (!stack.isEmpty()) {
                consumed.add(BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()));
                stack.shrink(1);
                if (stack.isEmpty()) {
                    additiveInventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        if (consumed.isEmpty()) {
            return; // 无添加剂，走原版逻辑
        }

        ItemStack ammo = new ItemStack(ModItems.MIXED_SNOWBALL);
        ammo.set(ModDataComponents.MIXED_ITEMS, consumed);

        ServerLevel serverLevel = (ServerLevel) this.level();
        double xd = target.getX() - this.getX();
        double yd = target.getEyeY() - 1.1;
        double zd = target.getZ() - this.getZ();
        double yo = Math.sqrt(xd * xd + zd * zd) * 0.2;

        Projectile.spawnProjectile(
                new MixedSnowballEntity(serverLevel, this, ammo),
                serverLevel,
                ammo,
                projectile -> projectile.shoot(xd, yd + yo - projectile.getY(), zd, 1.6f, 12.0f)
        );

        this.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0f, 0.4f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
        ci.cancel();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSave(ValueOutput output, CallbackInfo ci) {
        additiveInventory.storeAsItemList(output.list("AdditiveInventory", ItemStack.CODEC));
        output.putInt("SnowballCount", snowballCount);
        output.putInt("HealCooldown", healCooldown);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoad(ValueInput input, CallbackInfo ci) {
        additiveInventory.clearContent();
        input.list("AdditiveInventory", ItemStack.CODEC).ifPresent(stacks -> {
            for (ItemStack stack : stacks) {
                additiveInventory.addItem(stack);
            }
        });
        snowballCount = input.getIntOr("SnowballCount", 0);
        healCooldown = input.getIntOr("HealCooldown", 0);
        applyMaxHealthBonus();
    }

    @Unique
    private boolean hasBlazePowder() {
        for (int i = 0; i < 5; i++) {
            if (additiveInventory.getItem(i).is(Items.BLAZE_POWDER)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private int snowballHealthBonus() {
        return snowballCount / SNOWBALLS_PER_HEALTH;
    }

    /**
     * 雪球数量变化后更新血量上限。
     * 增加时同步治疗差额，减少时若当前血量超出上限则截断。
     */
    @Unique
    private void onSnowballCountChanged(int oldCount) {
        int oldBonus = oldCount / SNOWBALLS_PER_HEALTH;
        int newBonus = snowballCount / SNOWBALLS_PER_HEALTH;
        if (oldBonus == newBonus) return;

        var attr = this.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            float newMax = BASE_HEALTH + newBonus;
            attr.setBaseValue(newMax);
            if (newBonus > oldBonus) {
                this.heal(newBonus - oldBonus);
            } else if (this.getHealth() > newMax) {
                this.setHealth(newMax);
            }
        }
    }

    /**
     * 加载时直接应用血量上限加成（不触发治疗/截断，因为血量也会从NBT加载）。
     */
    @Unique
    private void applyMaxHealthBonus() {
        var attr = this.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(BASE_HEALTH + snowballHealthBonus());
        }
    }
}
