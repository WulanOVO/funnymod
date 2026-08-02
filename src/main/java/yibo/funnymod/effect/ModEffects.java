package yibo.funnymod.effect;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import yibo.funnymod.Funnymod;

public class ModEffects {
    public static final ResourceKey<MobEffect> INK_BLIND_KEY =
            ResourceKey.create(Registries.MOB_EFFECT, Funnymod.id("ink_blind"));

    public static final MobEffect INK_BLIND = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            INK_BLIND_KEY,
            new InkBlindEffect()
    );

    public static void initialize() {
        Funnymod.LOGGER.info("墨水遮挡效果已注册！");
    }
}
