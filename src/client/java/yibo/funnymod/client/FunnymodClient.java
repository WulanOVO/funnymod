package yibo.funnymod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import yibo.funnymod.Funnymod;
import yibo.funnymod.client.render.InkOverlay;
import yibo.funnymod.client.render.SnowballTintSource;
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
		// 注册墨水遮挡屏幕覆盖层
        HudElementRegistry.addFirst(
            Funnymod.id("ink_overlay"),
            InkOverlay::render
        );
	}
}