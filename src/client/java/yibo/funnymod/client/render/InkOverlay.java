package yibo.funnymod.client.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffectInstance;
import yibo.funnymod.effect.ModEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InkOverlay {
    /** 追踪每个玩家效果的最大持续时间，用于判断效果何时开始 */
    private static final Map<UUID, Integer> MAX_DURATIONS = new HashMap<>();

    /** 效果结束前开始渐消的tick数 */
    private static final int FADE_OUT_TICKS = 40;
    /** 基础开头黑障tick数（等级 0），每升一级 +10 tick */
    private static final int BASE_FLASH_TICKS = 10;
    /** 平台期透明度 */
    private static final float PLATEAU_ALPHA = 0.65f;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        MobEffectInstance effect = client.player.getEffect(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INK_BLIND)
        );

        UUID uuid = client.player.getUUID();

        if (effect == null || effect.isInfiniteDuration()) {
            MAX_DURATIONS.remove(uuid);
            return;
        }

        int remaining = effect.getDuration();
        if (remaining <= 0) return;

        // 追踪最大持续时间（效果刷新时更新）
        int maxDuration;
        Integer stored = MAX_DURATIONS.get(uuid);
        if (stored == null || remaining > stored) {
            maxDuration = remaining;
            MAX_DURATIONS.put(uuid, maxDuration);
        } else {
            maxDuration = stored;
        }

        float alpha = getAlpha(maxDuration, remaining, effect);

        if (alpha <= 0.005f) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        int a = (int) (alpha * 255);
        int color = ARGB.color(a, 0, 0, 0);
        graphics.fill(0, 0, screenW, screenH, color);
    }

    private static float getAlpha(int maxDuration, int remaining, MobEffectInstance effect) {
        int elapsed = maxDuration - remaining;
        int amplifier = effect.getAmplifier(); // 0-based: 等级0=0, 等级I=1, ...
        int flashTicks = BASE_FLASH_TICKS + amplifier * 10;
        float alpha;

        if (remaining <= FADE_OUT_TICKS) {
            // 最后阶段：从平台期渐消至完全透明
            alpha = PLATEAU_ALPHA * ((float) remaining / FADE_OUT_TICKS);
        } else if (elapsed <= flashTicks) {
            // 开头瞬间：从全黑(1.0)快速降到平台期
            float t = (float) elapsed / flashTicks;
            alpha = 1.0f - (1.0f - PLATEAU_ALPHA) * t;
        } else {
            // 平台期
            alpha = PLATEAU_ALPHA;
        }
        return alpha;
    }
}
