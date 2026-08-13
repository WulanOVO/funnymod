package yibo.funnymod.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 骷髅马烟花火箭槽位：只允许放入烟花火箭，最多堆叠 64 个。
 * 空槽时显示烟花火箭轮廓图标（位于 assets/funnymod/textures/gui/sprites/container/slot/firework_rocket.png）。
 */
public class FireworkSlot extends Slot {
    /** 烟花火箭槽位轮廓 sprite id */
    private static final Identifier EMPTY_ICON = Identifier.fromNamespaceAndPath("funnymod", "container/slot/firework_rocket");

    public FireworkSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(Items.FIREWORK_ROCKET);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public Identifier getNoItemIcon() {
        return EMPTY_ICON;
    }
}
