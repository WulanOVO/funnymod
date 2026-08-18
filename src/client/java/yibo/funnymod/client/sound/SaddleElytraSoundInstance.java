package yibo.funnymod.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * 坐骑滑翔音效，复刻原版 {@code ElytraOnPlayerSoundInstance}：
 * 循环播放，音量随速度变化，前 20 tick 静音、20~40 tick 淡入。
 * 用于装备鞍鞘的骷髅马等坐骑滑翔时。
 */
public class SaddleElytraSoundInstance extends AbstractTickableSoundInstance {
    private final LivingEntity entity;
    private int time;

    public SaddleElytraSoundInstance(LivingEntity entity) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1f;
    }

    @Override
    public void tick() {
        ++this.time;
        if (this.entity.isRemoved() || this.time > 20 && !this.entity.isFallFlying()) {
            this.stop();
            return;
        }
        this.x = (float) this.entity.getX();
        this.y = (float) this.entity.getY();
        this.z = (float) this.entity.getZ();
        float speed = (float) this.entity.getDeltaMovement().lengthSqr();
        this.volume = (double) speed >= 1.0E-7 ? Mth.clamp(speed / 4.0f, 0.0f, 1.0f) : 0.0f;
        if (this.time < 20) {
            this.volume = 0.0f;
        } else if (this.time < 40) {
            this.volume *= (float) (this.time - 20) / 20.0f;
        }
        this.pitch = this.volume > 0.8f ? 1.0f + (this.volume - 0.8f) : 1.0f;
    }
}
