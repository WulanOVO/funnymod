package yibo.funnymod.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import yibo.funnymod.Funnymod;
import yibo.funnymod.entity.ModEntities;

public class FunnymodClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 注册实体渲染器（复用原版雪球渲染）
		EntityRenderers.register(ModEntities.MIXED_SNOWBALL, ThrownItemRenderer::new);
		// 注册雪球颜色渲染
		ItemTintSources.ID_MAPPER.put(
				Funnymod.id("snowball_mix"),
				SnowballTintSource.MAP_CODEC
		);
	}
}