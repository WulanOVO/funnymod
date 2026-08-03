package yibo.funnymod.component;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import yibo.funnymod.Funnymod;

import java.util.List;

public class ModDataComponents {
    public static final DataComponentType<List<Holder<Item>>> MIXED_ITEMS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Funnymod.id("mixed_items"),
            DataComponentType.<List<Holder<Item>>>builder()
                    .persistent(Item.CODEC.listOf())
                    .networkSynchronized(Item.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build()
    );

    public static final int MAX_MIXED_ITEMS = 5;

    public static void initialize() {
    }
}
