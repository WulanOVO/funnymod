package yibo.funnymod.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import yibo.funnymod.Funnymod;

public class ModEntities {
    public static final ResourceKey<EntityType<?>> MIXED_SNOWBALL_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Funnymod.id("mixed_snowball"));

    public static final EntityType<MixedSnowballEntity> MIXED_SNOWBALL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MIXED_SNOWBALL_KEY,
            EntityType.Builder.<MixedSnowballEntity>of(MixedSnowballEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(MIXED_SNOWBALL_KEY)
    );

    // 可疑的雪傀儡
    public static final ResourceKey<EntityType<?>> SUSPICIOUS_SNOW_GOLEM_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Funnymod.id("suspicious_snow_golem"));

    public static final EntityType<SuspiciousSnowGolemEntity> SUSPICIOUS_SNOW_GOLEM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            SUSPICIOUS_SNOW_GOLEM_KEY,
            EntityType.Builder.<SuspiciousSnowGolemEntity>of(SuspiciousSnowGolemEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 1.9f)
                    .clientTrackingRange(8)
                    .build(SUSPICIOUS_SNOW_GOLEM_KEY)
    );

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(SUSPICIOUS_SNOW_GOLEM,
                SuspiciousSnowGolemEntity.createAttributes());
        SpawnPlacements.register(SUSPICIOUS_SNOW_GOLEM,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules);
    }
}
