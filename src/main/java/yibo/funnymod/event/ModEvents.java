package yibo.funnymod.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import yibo.funnymod.Funnymod;
import yibo.funnymod.entity.ModEntities;
import yibo.funnymod.entity.SuspiciousSnowGolemEntity;

public class ModEvents {
    private static final TagKey<Biome> SNOW_GOLEM_SPAWNS =
        TagKey.create(Registries.BIOME, Funnymod.id("snow_golem_spawns"));

    /** 「天马行空」进度：需保持腾空的 tick 数（10 秒 = 200 tick） */
    private static final int AIRBORNE_TICKS = 200;

    /** 记录骑乘者第一次释放烟花火箭的游戏时间（key=玩家 UUID，value=起始 gameTime） */
    private static final Map<UUID, Long> AIRBORNE_START = new HashMap<>();

    private static final int SPAWN_INTERVAL = 600;       // 每 30 秒尝试一次
    private static final int SPAWN_ATTEMPTS = 4;         // 每次尝试 4 个位置
    private static final int SPAWN_RANGE = 48;           // 生成范围：玩家周围 48 格
    private static final int DENSITY_RANGE = 64;         // 密度限制：64 格内最多 1 只
    private static final float SUSPICIOUS_CHANCE = 0.2f; // 20% 概率出可疑的

    public static void initialize() {
        // 雪块右键苦力怕 → 变成可疑的雪傀儡
        UseEntityCallback.EVENT.register(ModEvents::onUseEntity);

        // 手动 tick 生成：绕过 Fabric API 的 MISC 限制，同时生成原版雪傀儡和可疑雪傀儡
        ServerTickEvents.END_LEVEL_TICK.register(ModEvents::onLevelTick);
    }

    /**
     * 骷髅马释放烟花火箭时由 {@code SkeletonHorseMixin} 调用（服务端）。
     * 记录骑乘者第一次释放烟花火箭的游戏时间，用于「天马行空」进度的腾空计时。
     */
    public static void onDashStarted(AbstractHorse horse) {
        if (horse.getFirstPassenger() instanceof ServerPlayer player) {
            AIRBORNE_START.putIfAbsent(player.getUUID(), horse.level().getGameTime());
        }
    }

    /** 检测「天马行空」进度：骑乘骷髅马，第一次释放烟花火箭后 10 秒内均未触地 */
    private static void checkAirborneAdvancement(ServerLevel level) {
        if (AIRBORNE_START.isEmpty()) return;
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            Long startTick = AIRBORNE_START.get(player.getUUID());
            if (startTick == null) continue;

            Entity vehicle = player.getVehicle();
            if (!(vehicle instanceof SkeletonHorse)) {
                AIRBORNE_START.remove(player.getUUID()); // 下马或换坐骑，重置
                continue;
            }
            if (vehicle.onGround()) {
                AIRBORNE_START.remove(player.getUUID()); // 触地，重置
                continue;
            }
            if (now - startTick >= AIRBORNE_TICKS) {
                awardPegasus(level, player);
                AIRBORNE_START.remove(player.getUUID());
            }
        }
    }

    /** 授予「天马行空」进度 */
    private static void awardPegasus(ServerLevel level, ServerPlayer player) {
        AdvancementHolder holder = level.getServer().getAdvancements().get(Funnymod.id("pegasus"));
        if (holder != null) {
            player.getAdvancements().award(holder, "airborne_10s");
        }
    }

    private static InteractionResult onUseEntity(
        Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult entityHitResult
    ) {
        if (!(entity instanceof Creeper creeper)) return InteractionResult.PASS;
        if (creeper instanceof SuspiciousSnowGolemEntity) return InteractionResult.PASS;
        if (!player.getItemInHand(hand).is(Items.SNOW_BLOCK)) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS; // 客户端返回 SUCCESS 触发伸手动画

        SuspiciousSnowGolemEntity snowGolem = ModEntities.SUSPICIOUS_SNOW_GOLEM
            .create(level, EntitySpawnReason.CONVERSION);
        if (snowGolem == null) return InteractionResult.PASS;

        level.addFreshEntity(snowGolem);
        snowGolem.absSnapTo(
            creeper.getX(), creeper.getY(), creeper.getZ(),
            creeper.getYRot(), creeper.getXRot()
        );
        snowGolem.setYHeadRot(creeper.getYHeadRot());
        snowGolem.setYBodyRot(creeper.yBodyRot);
        snowGolem.setHealth(snowGolem.getMaxHealth());
        snowGolem.setTarget(creeper.getTarget());

        if (creeper.hasCustomName()) {
            snowGolem.setCustomName(creeper.getCustomName());
            snowGolem.setCustomNameVisible(creeper.isCustomNameVisible());
        }

        level.addFreshEntity(snowGolem);
        snowGolem.markPlayerCreated();
        creeper.discard();

        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        level.playSound(
            null, snowGolem.getX(), snowGolem.getY(), snowGolem.getZ(),
            SoundEvents.SNOW_PLACE, snowGolem.getSoundSource(), 1.0f, 1.0f
        );

        return InteractionResult.SUCCESS;
    }

    private static void onLevelTick(ServerLevel level) {
        checkAirborneAdvancement(level);

        if (level.getGameTime() % SPAWN_INTERVAL != 0) return;
        if (level.players().isEmpty()) return;

        var random = level.getRandom();
        var player = level.players().get(random.nextInt(level.players().size()));
        BlockPos playerPos = player.blockPosition();

        // 检查玩家是否在寒冷群系
        if (!level.getBiome(playerPos).is(SNOW_GOLEM_SPAWNS)) return;

        // 密度限制：64 格内已有同类则不生成
        if (countNearby(level, playerPos) > 0) return;

        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            int x = playerPos.getX() + random.nextInt(SPAWN_RANGE) - SPAWN_RANGE / 2;
            int z = playerPos.getZ() + random.nextInt(SPAWN_RANGE) - SPAWN_RANGE / 2;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos spawnPos = new BlockPos(x, y, z);

            // 必须在寒冷群系
            if (!level.getBiome(spawnPos).is(SNOW_GOLEM_SPAWNS)) continue;

            // 验证生成位置
            if (!Mob.checkMobSpawnRules(
                EntityTypes.SNOW_GOLEM, level,
                EntitySpawnReason.NATURAL, spawnPos, random
            )) {
                continue;
            }

            if (random.nextFloat() < SUSPICIOUS_CHANCE) {
                // 20% 可疑雪傀儡
                SuspiciousSnowGolemEntity ssg = ModEntities.SUSPICIOUS_SNOW_GOLEM
                    .create(level, EntitySpawnReason.NATURAL);
                if (ssg != null) {
                    ssg.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    level.addFreshEntity(ssg);
                }
            } else {
                // 80% 原版雪傀儡
                SnowGolem sg = EntityTypes.SNOW_GOLEM.create(level, EntitySpawnReason.NATURAL);
                if (sg != null) {
                    sg.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    level.addFreshEntity(sg);
                }
            }
            break; // 每次 tick 最多生成 1 只
        }
    }

    private static int countNearby(ServerLevel world, BlockPos center) {
        AABB box = new AABB(center).inflate(DENSITY_RANGE);
        int count = 0;
        for (SnowGolem ignored : world.getEntitiesOfClass(SnowGolem.class, box)) {
            count++;
        }
        for (SuspiciousSnowGolemEntity ignored : world.getEntitiesOfClass(SuspiciousSnowGolemEntity.class, box)) {
            count++;
        }
        return count;
    }
}
