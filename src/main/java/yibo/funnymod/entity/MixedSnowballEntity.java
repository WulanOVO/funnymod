package yibo.funnymod.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import yibo.funnymod.component.ModDataComponents;
import yibo.funnymod.item.ModItems;

public class MixedSnowballEntity extends ThrowableItemProjectile {

    public MixedSnowballEntity(EntityType<? extends MixedSnowballEntity> type, Level level) {
        super(type, level);
    }

    public MixedSnowballEntity(Level level, LivingEntity owner, ItemStack itemStack) {
        super(ModEntities.MIXED_SNOWBALL, owner, level, itemStack);
    }

    public MixedSnowballEntity(Level level, double x, double y, double z, ItemStack itemStack) {
        super(ModEntities.MIXED_SNOWBALL, x, y, z, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MIXED_SNOWBALL;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        var mixedItems = this.getItem().get(ModDataComponents.MIXED_ITEMS);
        if (mixedItems == null || mixedItems.isEmpty()) return;

        int flintCount = 0;
        int blazeCount = 0;
        for (var holder : mixedItems) {
            if (holder.value() == Items.FLINT) flintCount++;
            if (holder.value() == Items.BLAZE_POWDER) blazeCount++;
        }

        Entity target = hitResult.getEntity();

        if (flintCount > 0) {
            target.hurt(this.damageSources().thrown(this, this.getOwner()), flintCount + 1);
        }

        if (blazeCount > 0 && target instanceof LivingEntity living) {
            living.igniteForSeconds(blazeCount * 1.5f + 1);
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            var mixedItems = this.getItem().get(ModDataComponents.MIXED_ITEMS);
            if (mixedItems != null) {
                boolean hasFlint = false;
                int blazeCount = 0;
                int gunpowderCount = 0;
                for (var holder : mixedItems) {
                    if (holder.value() == Items.FLINT) hasFlint = true;
                    if (holder.value() == Items.BLAZE_POWDER) blazeCount++;
                    if (holder.value() == Items.GUNPOWDER) gunpowderCount++;
                }

                if (hasFlint) {
                    float roll = this.random.nextFloat();
                    if (roll < 0.20f) {
                        this.level().addFreshEntity(new ItemEntity(
                                this.level(),
                                this.getX(), this.getY(), this.getZ(),
                                new ItemStack(Items.FLINT)
                        ));
                    }
                }

                if (blazeCount > 0) {
                    if (blazeCount > 2) {
                        BlockState state = level().getBlockState(this.blockPosition());
                        if (state.isAir() || state.canBeReplaced()) {
                            level().setBlock(this.blockPosition(), Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }

                    if (gunpowderCount > 0) {
                        boolean enableFire = blazeCount > 1;
                        level().explode(
                                this.getOwner(),
                                damageSources().thrown(this, this.getOwner()), null,
                                this.getX(), this.getY(), this.getZ(),
                                (float) gunpowderCount * 0.3f, enableFire, Level.ExplosionInteraction.BLOCK
                        );
                    }
                }
            }

            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
