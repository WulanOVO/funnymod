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
    public static final ResourceKey<MobEffect> STICKY_KEY =
            ResourceKey.create(Registries.MOB_EFFECT, Funnymod.id("sticky"));
    public static final ResourceKey<MobEffect> STICKY_FEET_KEY =
            ResourceKey.create(Registries.MOB_EFFECT, Funnymod.id("sticky_feet"));

    public static final MobEffect INK_BLIND = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            INK_BLIND_KEY,
            new InkBlindEffect()
    );
    public static final MobEffect STICKY = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            STICKY_KEY,
            new StickyEffect()
    );
    public static final MobEffect STICKY_FEET = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            STICKY_FEET_KEY,
            new StickyFeetEffect()
    );

    public static void initialize() {
    }
}
