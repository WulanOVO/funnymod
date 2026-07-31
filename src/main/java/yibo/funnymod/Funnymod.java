package yibo.funnymod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yibo.funnymod.enchantment.ModEnchantments;

public class Funnymod implements ModInitializer {
	public static final String MOD_ID = "funnymod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEnchantments.initialize();
		LOGGER.info("FunnyMod 初始化完成！拽引附魔已就绪~");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
