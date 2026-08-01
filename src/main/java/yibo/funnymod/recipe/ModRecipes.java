package yibo.funnymod.recipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import yibo.funnymod.Funnymod;

public class ModRecipes {
    public static void initialize() {
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Funnymod.id("snowball_mix"),
                SnowballMixRecipe.SERIALIZER
        );
        Funnymod.LOGGER.info("雪球混合配方已注册！");
    }
}
