package yibo.funnymod.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

    public static void initialize() {
        Funnymod.LOGGER.info("混合雪球实体已注册！");
    }
}
