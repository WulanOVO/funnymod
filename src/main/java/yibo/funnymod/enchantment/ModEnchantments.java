package yibo.funnymod.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import yibo.funnymod.Funnymod;

/**
 * 模组附魔定义。
 * 附魔本身通过 data/funnymod/enchantment/grapple.json 数据驱动注册。
 * 这里只保留 RegistryKey 供代码中引用。
 */
public class ModEnchantments {
    public static final ResourceKey<Enchantment> GRAPPLE =
            ResourceKey.create(Registries.ENCHANTMENT, Funnymod.id("grapple"));

    public static void initialize() {
        // 数据驱动附魔不需要代码注册，这里仅做类加载触发
        Funnymod.LOGGER.info("拽引附魔 Key 已就绪！");
    }
}
