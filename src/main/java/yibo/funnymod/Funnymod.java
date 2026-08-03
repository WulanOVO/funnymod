package yibo.funnymod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yibo.funnymod.component.ModDataComponents;
import yibo.funnymod.effect.ModEffects;
import yibo.funnymod.enchantment.ModEnchantments;
import yibo.funnymod.entity.ModEntities;
import yibo.funnymod.item.ModItems;
import yibo.funnymod.recipe.ModRecipes;

public class Funnymod implements ModInitializer {
	public static final String MOD_ID = "funnymod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEnchantments.initialize();
		ModDataComponents.initialize();
		ModEffects.initialize();
		ModItems.initialize();
		ModEntities.initialize();
		ModRecipes.initialize();
		LOGGER.info("FunnyMod 初始化完成！");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
