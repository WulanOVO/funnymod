package yibo.funnymod.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import yibo.funnymod.Funnymod;

public class ModItems {
    public static final ResourceKey<Item> MIXED_SNOWBALL_KEY =
            ResourceKey.create(Registries.ITEM, Funnymod.id("mixed_snowball"));

    public static final Item MIXED_SNOWBALL = Registry.register(
            BuiltInRegistries.ITEM,
            MIXED_SNOWBALL_KEY,
            new MixedSnowballItem(new Item.Properties()
                    .setId(MIXED_SNOWBALL_KEY)
                    .stacksTo(16))
    );

    public static void initialize() {
        // 注册发射器行为，让混合雪球能被发射器射出
        DispenserBlock.registerProjectileBehavior(MIXED_SNOWBALL);
        Funnymod.LOGGER.info("混合雪球物品已注册！");
    }
}
