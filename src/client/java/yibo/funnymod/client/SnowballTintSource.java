package yibo.funnymod.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import yibo.funnymod.item.MixedSnowballItem;

/**
 * 雪球混合颜色源：根据掺入物品数量决定颜色
 */
public record SnowballTintSource() implements ItemTintSource {
    public static final MapCodec<SnowballTintSource> MAP_CODEC = MapCodec.unit(new SnowballTintSource());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        return MixedSnowballItem.getColorForMixedCount(MixedSnowballItem.getMixedCount(stack));
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
