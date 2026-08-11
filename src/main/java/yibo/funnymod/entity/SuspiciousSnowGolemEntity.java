package yibo.funnymod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import yibo.funnymod.Funnymod;

public class SuspiciousSnowGolemEntity extends Creeper {
    private static final EntityDataAccessor<Byte> DATA_PUMPKIN_ID =
            SynchedEntityData.defineId(SuspiciousSnowGolemEntity.class, EntityDataSerializers.BYTE);
    private static final byte PUMPKIN_FLAG = 16;
    private static final int MAX_SNOW_LAYER = 4;
    private static final int MELT_COOLDOWN = 20; // 炎热环境每秒减 1 点雪层

    private int snowLayerHealth = MAX_SNOW_LAYER;
    private int meltCooldown = MELT_COOLDOWN;
    private boolean playerCreated = false;
    private boolean firstTickDone = false;

    public SuspiciousSnowGolemEntity(EntityType<? extends SuspiciousSnowGolemEntity> type, Level level) {
        super(type, level);
    }

    public void markPlayerCreated() {
        playerCreated = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PUMPKIN_ID, (byte) 0);
    }

    public boolean hasPumpkin() {
        return (entityData.get(DATA_PUMPKIN_ID) & PUMPKIN_FLAG) != 0;
    }

    public void setPumpkin(boolean pumpkin) {
        byte current = entityData.get(DATA_PUMPKIN_ID);
        if (pumpkin) {
            entityData.set(DATA_PUMPKIN_ID, (byte) (current | PUMPKIN_FLAG));
        } else {
            entityData.set(DATA_PUMPKIN_ID, (byte) (current & ~PUMPKIN_FLAG));
        }
    }

    /** 像雪傀儡一样怕水 */
    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    /** 受到伤害时同步削减雪层；水伤只扣雪层不穿透本体血量 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 水伤（雨/水/喷溅水瓶）：只削减雪层，不扣本体 HP
        if (source.is(DamageTypes.DROWN) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            if (snowLayerHealth > 0) {
                reduceSnowLayer(level, (int) Math.ceil(amount));
            }
            return false;
        }
        // 其他伤害：正常扣 HP + 同步削减雪层
        boolean damaged = super.hurtServer(level, source, amount);
        if (damaged && snowLayerHealth > 0) {
            reduceSnowLayer(level, (int) Math.ceil(amount));
        }
        return damaged;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level() instanceof ServerLevel serverLevel) {
            // 首次 tick：自然生成的会戴南瓜伪装成普通雪傀儡
            if (!firstTickDone) {
                firstTickDone = true;
                if (!playerCreated) {
                    setPumpkin(true);
                }
            }

            // 积雪路径
            if (serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
                BlockState snow = Blocks.SNOW.defaultBlockState();
                for (int i = 0; i < 4; ++i) {
                    int xx = Mth.floor(getX() + (double) ((float) (i % 2 * 2 - 1) * 0.25f));
                    int yy = Mth.floor(getY());
                    int zz = Mth.floor(getZ() + (double) ((float) (i / 2 % 2 * 2 - 1) * 0.25f));
                    BlockPos snowPos = new BlockPos(xx, yy, zz);
                    if (!level().getBlockState(snowPos).isAir() || !snow.canSurvive(level(), snowPos)) {
                        continue;
                    }
                    level().setBlockAndUpdate(snowPos, snow);
                    level().gameEvent(GameEvent.BLOCK_PLACE, snowPos, GameEvent.Context.of(this, snow));
                }
            }

            // 炎热环境：每秒削减 1 点雪层（与雪傀儡受伤条件一致）
            if (--meltCooldown <= 0) {
                meltCooldown = MELT_COOLDOWN;
                if (serverLevel.environmentAttributes()
                        .getValue(EnvironmentAttributes.SNOW_GOLEM_MELTS, position()).booleanValue()) {
                    reduceSnowLayer(serverLevel, 1);
                }
            }
        }
    }

    private void reduceSnowLayer(ServerLevel level, int amount) {
        if (snowLayerHealth <= 0) return;
        snowLayerHealth -= amount;
        if (snowLayerHealth <= 0) {
            snowLayerHealth = 0;
            revertToCreeper(level);
        } else {
            // 雪层剥落粒子
            Vec3 pos = position();
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    pos.x, pos.y + 1.0, pos.z,
                    5, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private void revertToCreeper(ServerLevel serverLevel) {
        Creeper creeper = EntityTypes.CREEPER.create(serverLevel, EntitySpawnReason.CONVERSION);
        if (creeper == null) return;

        creeper.setPos(getX(), getY(), getZ());
        creeper.setYRot(getYRot());
        creeper.setXRot(getXRot());
        creeper.setTarget(getTarget());
        if (hasCustomName()) {
            creeper.setCustomName(getCustomName());
            creeper.setCustomNameVisible(isCustomNameVisible());
        }

        // 掉落南瓜
        if (hasPumpkin()) {
            spawnAtLocation(serverLevel, new ItemStack(Items.CARVED_PUMPKIN));
        }

        // 雪层破碎水花
        Vec3 pos = position();
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                pos.x, pos.y + 0.5, pos.z,
                15, 0.5, 0.3, 0.5, 0.05);
        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.SNOW_BREAK, getSoundSource(), 0.5f, 1.5f);

        serverLevel.addFreshEntity(creeper);
        discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        // 南瓜盖头
        if (itemStack.is(Items.CARVED_PUMPKIN) && !hasPumpkin()) {
            if (!level().isClientSide()) {
                setPumpkin(true);
                itemStack.consume(1, player);
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.SNOW_PLACE, getSoundSource(), 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        // 剪刀摘南瓜
        if (itemStack.is(Items.SHEARS) && hasPumpkin()) {
            if (!level().isClientSide()) {
                setPumpkin(false);
                spawnAtLocation((ServerLevel) level(), new ItemStack(Items.CARVED_PUMPKIN));
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.SNOW_GOLEM_SHEAR, getSoundSource(), 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Pumpkin", hasPumpkin());
        output.putInt("SnowLayer", snowLayerHealth);
        output.putInt("MeltCooldown", meltCooldown);
        output.putBoolean("PlayerCreated", playerCreated);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setPumpkin(input.getBooleanOr("Pumpkin", false));
        snowLayerHealth = input.getIntOr("SnowLayer", MAX_SNOW_LAYER);
        meltCooldown = input.getIntOr("MeltCooldown", MELT_COOLDOWN);
        playerCreated = input.getBooleanOr("PlayerCreated", false);
    }
}
