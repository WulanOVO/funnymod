package yibo.funnymod.entity.goal;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import yibo.funnymod.Funnymod;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * 让雪傀儡主动走向附近带有 snow_golem_picks_up 标签的物品实体。
 * 当物品进入 aiStep 拾取范围后，由原版 Mob.aiStep() 触发实际拾取。
 */
public class SnowGolemPickUpGoal extends Goal {
    private static final TagKey<Item> SNOW_GOLEM_PICKS_UP = TagKey.create(
        Registries.ITEM, Funnymod.id("snow_golem_picks_up"));

    private final Mob mob;
    private final double speedModifier;
    private final double searchRange;
    @Nullable
    private ItemEntity targetItem;

    public SnowGolemPickUpGoal(Mob mob, double speedModifier, double searchRange) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRange = searchRange;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        List<ItemEntity> items = mob.level().getEntitiesOfClass(
            ItemEntity.class,
            mob.getBoundingBox().inflate(searchRange),
            item -> {
                if (!item.isAlive() || item.hasPickUpDelay()) return false;
                ItemStack stack = item.getItem();
                return !stack.isEmpty() && stack.is(SNOW_GOLEM_PICKS_UP);
            }
        );

        if (items.isEmpty()) return false;

        // 按距离排序，取最近的
        items.sort(Comparator.comparingDouble(mob::distanceToSqr));
        targetItem = items.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetItem != null
               && targetItem.isAlive()
               && !targetItem.hasPickUpDelay()
               && mob.distanceToSqr(targetItem) > 2.0; // 足够近时停止，交给 aiStep 拾取
    }

    @Override
    public void start() {
        if (targetItem != null) {
            mob.getNavigation().moveTo(targetItem, speedModifier);
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetItem != null && mob.distanceToSqr(targetItem) < 4.0) {
            mob.getLookControl().setLookAt(targetItem);
            mob.getNavigation().moveTo(targetItem, speedModifier);
        }
    }
}
